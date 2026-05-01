package org.odenix.mill.github

import mill.*
import org.odenix.mill.BuildVariable
import org.odenix.mill.ReleaseNotes

trait ReleaseModule extends Module {
  private def ReleaseVersion = BuildVariable("RELEASE_VERSION")

  def releaseVersion = Task.Input {
    ReleaseVersion.require(Task.env)
  }

  private def changelog = Task.Source {
    moduleDir / "CHANGELOG.md"
  }

  def releaseNotes = Task {
    ReleaseNotes.fromChangelog(changelog().path, releaseVersion())
  }

  def printReleaseNotes() = Task.Command {
    println(releaseNotes())
  }

  def upsertRelease() = Task.Command {
    val repo = GitHubEnv.Repository.require(Task.env)
    val version = releaseVersion()
    Release.upsert(repo, version, releaseNotes(), GitHubEnv.Token.require(Task.env))
    println(s"Uploaded GitHub release notes for $repo@$version")
  }
}
