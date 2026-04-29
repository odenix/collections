package org.odenix.mill

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import scala.util.Using

object MavenCentral {
  enum ArtifactState {
    case Present
    case Missing
    case Unknown(status: Int, body: String)
  }

  def getArtifactState(url: String): ArtifactState = {
    Using.resource(HttpClient.newHttpClient()) { client =>
      val response =
        client.send(
          HttpRequest
            .newBuilder(URI.create(url))
            .GET()
            .build(),
          HttpResponse.BodyHandlers.ofString()
        )

      response.statusCode() match {
        case 200 => ArtifactState.Present
        case 404 => ArtifactState.Missing
        case status => ArtifactState.Unknown(status, response.body())
      }
    }
  }

  def createPomUrl(groupId: String, artifactId: String, version: String): String = {
    val groupPath = groupId.replace('.', '/')
    s"https://repo1.maven.org/maven2/$groupPath/$artifactId/$version/$artifactId-$version.pom"
  }
}
