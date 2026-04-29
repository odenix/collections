package org.odenix.mill

import upickle.default.*

import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.net.{URI, URLEncoder}
import java.nio.charset.StandardCharsets
import scala.util.Using

final case class GitHubRelease(
    id: Long,
    tag_name: String,
    name: String,
    upload_url: String
) derives Reader

object GitHubRelease {
  def upsert(repository: String, version: String, notes: String, token: String): GitHubRelease = {
    Using.resource(HttpClient.newHttpClient()) { client =>
      findByTag(client, repository, version, token) match {
        case Some(release) => update(client, repository, release.id, version, notes, token)
        case None => create(client, repository, version, notes, token)
      }
    }
  }

  private def findByTag(
      client: HttpClient,
      repository: String,
      tagName: String,
      token: String
  ): Option[GitHubRelease] = {
    val uri = URI.create(s"https://api.github.com/repos/$repository/releases/tags/$tagName")
    val response =
      client.send(
        githubRequest(uri, token).GET().build(),
        HttpResponse.BodyHandlers.ofString()
      )

    response.statusCode() match {
      case 200 => Some(parseRelease(response.body(), s"release for tag $tagName"))
      case 404 => None
      case status =>
        sys.error(s"Could not fetch GitHub release for $tagName: HTTP $status\n${response.body()}")
    }
  }

  private def create(
      client: HttpClient,
      repository: String,
      version: String,
      notes: String,
      token: String
  ): GitHubRelease = {
    val uri = URI.create(s"https://api.github.com/repos/$repository/releases")
    val response =
      client.send(
        githubRequest(uri, token)
          .POST(HttpRequest.BodyPublishers.ofString(releasePayload(version, notes)))
          .build(),
        HttpResponse.BodyHandlers.ofString()
      )
    requireSuccess(response, "create GitHub release")
    parseRelease(response.body(), s"created release for tag $version")
  }

  private def update(
      client: HttpClient,
      repository: String,
      releaseId: Long,
      version: String,
      notes: String,
      token: String
  ): GitHubRelease = {
    val uri = URI.create(s"https://api.github.com/repos/$repository/releases/$releaseId")
    val response =
      client.send(
        githubRequest(uri, token)
          .method("PATCH", HttpRequest.BodyPublishers.ofString(releasePayload(version, notes)))
          .build(),
        HttpResponse.BodyHandlers.ofString()
      )
    requireSuccess(response, "update GitHub release")
    parseRelease(response.body(), s"updated release for tag $version")
  }

  private def parseRelease(body: String, description: String): GitHubRelease = {
    try {
      read[GitHubRelease](body)
    } catch {
      case e: upickle.core.Abort =>
        sys.error(s"Could not parse GitHub $description: ${e.getMessage}")
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

  private def releasePayload(version: String, notes: String): String = {
    write(ReleasePayload(tag_name = version, name = version, body = notes))
  }

  private def requireSuccess(response: HttpResponse[String], operation: String): Unit = {
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      sys.error(s"Failed to $operation: HTTP ${response.statusCode()}\n${response.body()}")
    }
  }

  private final case class ReleasePayload(
    tag_name: String,
    name: String,
    body: String
  ) derives Writer
}
