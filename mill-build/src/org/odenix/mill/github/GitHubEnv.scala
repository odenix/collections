package org.odenix.mill.github

import upickle.default.ReadWriter

final case class GitHubEnv(
    repository: Option[String],
    token: Option[String],
    sha: Option[String],
    ref: Option[String],
    runId: Option[String],
    workflow: Option[String],
    job: Option[String],
    serverUrl: Option[String]
) derives ReadWriter {
  def requireRepository(): String = repository.getOrElse(
    sys.error("GITHUB_REPOSITORY is required")
  )

  def requireToken(): String = token.getOrElse(
    sys.error("GITHUB_TOKEN or GH_TOKEN is required")
  )

  def requireSha(): String = sha.getOrElse(
    sys.error("GITHUB_SHA is required")
  )

  def requireRef(): String = ref.getOrElse(
    sys.error("GITHUB_REF is required")
  )
}

object GitHubEnv {
  def apply(env: Map[String, String]): GitHubEnv =
    GitHubEnv(
      repository = envValue(env, "GITHUB_REPOSITORY"),
      token = envValue(env, "GITHUB_TOKEN").orElse(envValue(env, "GH_TOKEN")),
      sha = envValue(env, "GITHUB_SHA"),
      ref = envValue(env, "GITHUB_REF"),
      runId = envValue(env, "GITHUB_RUN_ID"),
      workflow = envValue(env, "GITHUB_WORKFLOW"),
      job = envValue(env, "GITHUB_JOB"),
      serverUrl = envValue(env, "GITHUB_SERVER_URL")
    )

  private def envValue(env: Map[String, String], name: String): Option[String] =
    env.get(name).filterNot(_.isBlank)
}
