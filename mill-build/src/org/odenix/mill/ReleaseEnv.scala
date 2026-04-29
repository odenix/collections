package org.odenix.mill

import upickle.default.ReadWriter

final case class ReleaseEnv(
    version: Option[String],
    githubRepository: Option[String],
    githubToken: Option[String]
) derives ReadWriter {
  def requireVersion(): String = version.getOrElse(
    sys.error("RELEASE_VERSION is required")
  )

  def requireGitHubRepository(): String = githubRepository.getOrElse(
    sys.error("GITHUB_REPOSITORY is required to upload GitHub release notes")
  )

  def requireGitHubToken(): String = githubToken.getOrElse(
    sys.error("GITHUB_TOKEN or GH_TOKEN is required to upload GitHub release notes")
  )
}

object ReleaseEnv {
  def apply(env: Map[String, String]): ReleaseEnv =
    ReleaseEnv(
      version = versionFromEnv(env),
      githubRepository = env.get("GITHUB_REPOSITORY"),
      githubToken = env.get("GITHUB_TOKEN").orElse(env.get("GH_TOKEN"))
    )

  private def versionFromEnv(env: Map[String, String]): Option[String] = {
    // GitHub jobs set empty string if version is not available
    env.get("RELEASE_VERSION").filterNot(_.isBlank)
  }
}
