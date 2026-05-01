package org.odenix.mill.github

import org.odenix.mill.BuildVariable

private[github] object GitHubEnv {
  val Repository = BuildVariable("GITHUB_REPOSITORY")
  val Token = BuildVariable("GITHUB_TOKEN", "GH_TOKEN")
  val Sha = BuildVariable("GITHUB_SHA")
  val Ref = BuildVariable("GITHUB_REF")
  val RunId = BuildVariable("GITHUB_RUN_ID")
  val Workflow = BuildVariable("GITHUB_WORKFLOW")
  val Job = BuildVariable("GITHUB_JOB")
  val ServerUrl = BuildVariable("GITHUB_SERVER_URL")
}
