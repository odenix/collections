package org.odenix.mill.github

import mill.*
import mill.javalib.JavaModule
import upickle.default.*

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Instant
import scala.util.Using

final case class DependencyGraph(
    version: Int,
    sha: String,
    ref: String,
    job: DependencyGraph.Job,
    detector: DependencyGraph.Detector,
    scanned: String,
    manifests: Map[String, DependencyGraph.Manifest]
) derives ReadWriter

object DependencyGraph {
  enum Scope(val value: String) derives ReadWriter {
    case Runtime extends Scope("runtime")
    case Development extends Scope("development")
  }

  enum Relationship(val value: String) derives ReadWriter {
    case Direct extends Relationship("direct")
    case Indirect extends Relationship("indirect")
  }

  def fromEnv(
               env: Map[String, String],
               manifests: Seq[ManifestEntry],
               detector: Detector = Detector.Default
  ): DependencyGraph = {
    DependencyGraph(
      version = 0,
      sha = GitHubEnv.Sha.require(env),
      ref = GitHubEnv.Ref.require(env),
      job = Job.fromEnv(env),
      detector = detector,
      scanned = Instant.now().toString,
      manifests = manifests.map(entry => entry.key -> entry.manifest).toMap
    )
  }

  final case class ManifestEntry(
      key: String,
      manifest: Manifest
  ) derives ReadWriter

  object ManifestEntry {
    inline def fromJavaModule(
        inline module: JavaModule,
        scope: Scope,
        name: Option[String] = None,
        extraDependencies: Seq[String] = Seq.empty
    ): ManifestEntry = {
      val sourceLocation = sourceLocationFor(module)
      val directDependencies = module.mvnDeps().map(_.toString) ++ extraDependencies
      val resolvedDependencyPaths = module.resolvedMvnDeps().map(_.path.toString)
      val direct = directDependencies.flatMap(parseCoordinate).map(_.purl).toSet
      val coordinates = (
        directDependencies.flatMap(parseCoordinate) ++
          resolvedDependencyPaths.flatMap(coordinateFromCoursierPath)
      ).distinctBy(_.purl)

      ManifestEntry(
        key = defaultKey(module, sourceLocation),
        manifest = Manifest(
          name = name.getOrElse(defaultName(module)),
          file = ManifestFile(sourceLocation),
          resolved = coordinates.map { coordinate =>
            coordinate.purl -> ResolvedDependency(
              package_url = coordinate.purl,
              relationship = Some(if (direct.contains(coordinate.purl)) Relationship.Direct.value else Relationship.Indirect.value),
              scope = Some(scope.value)
            )
          }.toMap
        )
      )
    }

    private def defaultKey(module: JavaModule, sourceLocation: String): String = {
      module.moduleSegments.render match {
        case "" => sourceLocation
        case modulePath => modulePath
      }
    }

    private def defaultName(module: JavaModule): String = {
      module.moduleSegments.render match {
        case "" => "root"
        case modulePath => modulePath
      }
    }
  }

  final case class Manifest(
      name: String,
      file: ManifestFile,
      resolved: Map[String, ResolvedDependency]
  ) derives ReadWriter

  final case class ManifestFile(source_location: String) derives ReadWriter

  final case class ResolvedDependency(
      package_url: String,
      relationship: Option[String],
      scope: Option[String]
  ) derives ReadWriter

  final case class Job(
      id: String,
      correlator: String,
      html_url: Option[String]
  ) derives ReadWriter

  object Job {
    private[github] def fromEnv(env: Map[String, String]): Job = {
      val runUrl = for {
        serverUrl <- GitHubEnv.ServerUrl.get(env)
        repository <- GitHubEnv.Repository.get(env)
        runId <- GitHubEnv.RunId.get(env)
      } yield s"$serverUrl/$repository/actions/runs/$runId"

      Job(
        id = GitHubEnv.RunId.get(env).getOrElse("local"),
        correlator = Seq(GitHubEnv.Workflow.get(env), GitHubEnv.Job.get(env)).flatten.mkString(" "),
        html_url = runUrl
      )
    }
  }

  final case class Detector(
      name: String,
      version: String,
      url: String
  ) derives ReadWriter

  object Detector {
    val Default: Detector = Detector(
      name = "odenix-mill-dependency-graph",
      version = "1",
      url = "https://github.com/odenix/collections"
    )
  }

  def submit(repository: String, token: String, graph: DependencyGraph): Unit = {
    Using.resource(HttpClient.newHttpClient()) { client =>
      val response =
        client.send(
          githubRequest(URI.create(s"https://api.github.com/repos/$repository/dependency-graph/snapshots"), token)
            .POST(HttpRequest.BodyPublishers.ofString(write(graph)))
            .build(),
          HttpResponse.BodyHandlers.ofString()
        )
      if (response.statusCode() != 201) {
        sys.error(s"Failed to submit GitHub dependency graph: HTTP ${response.statusCode()}\n${response.body()}")
      }
    }
  }

  def submit(env: Map[String, String], graph: DependencyGraph): Unit = {
    submit(
      repository = GitHubEnv.Repository.require(env),
      token = GitHubEnv.Token.require(env),
      graph = graph
    )
  }

  private def sourceLocationFor(module: JavaModule): String = {
    val packageMill = module.moduleDir / "package.mill"
    val buildMill = module.moduleDir / "build.mill"
    val source = if (os.exists(packageMill)) packageMill else buildMill
    source.relativeTo(os.pwd).toString.replace('\\', '/')
  }

  private def parseCoordinate(value: String): Option[MavenCoordinate] = {
    value.split(':').toSeq match {
      case Seq(group, artifact, version) => Some(MavenCoordinate(group, artifact, version))
      case _ => None
    }
  }

  private def coordinateFromCoursierPath(value: String): Option[MavenCoordinate] = {
    val path = value
      .replaceFirst("^qref:v1:[^:]+:", "")
      .replace('\\', '/')
    val marker = "/maven2/"
    val index = path.indexOf(marker)
    if (index < 0) None
    else {
      val parts = path.substring(index + marker.length).split('/').toSeq
      if (parts.length < 4) None
      else {
        val artifact = parts(parts.length - 3)
        val version = parts(parts.length - 2)
        val group = parts.dropRight(3).mkString(".")
        Some(MavenCoordinate(group, artifact, version))
      }
    }
  }

  private def githubRequest(uri: URI, token: String): HttpRequest.Builder = {
    HttpRequest
      .newBuilder(uri)
      .header("Accept", "application/vnd.github+json")
      .header("Authorization", s"Bearer $token")
      .header("Content-Type", "application/json")
      .header("X-GitHub-Api-Version", "2022-11-28")
  }

  private final case class MavenCoordinate(group: String, artifact: String, version: String) {
    def purl: String = s"pkg:maven/$group/$artifact@$version"
  }
}
