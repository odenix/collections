package org.odenix.mill

import scala.jdk.CollectionConverters.*
import scala.jdk.StreamConverters.*

object ReleaseNotes {
  def fromChangelog(changelogPath: os.Path, version: String): String = {
    fromChangelog(os.read(changelogPath), version, changelogPath.toString)
  }

  def fromChangelog(changelog: String, version: String, displayName: String = "changelog"): String = {
    val lines = changelog.lines().toScala(Seq)
    val start = lines.indexWhere(line => extractVersion(line).contains(version))
    if (start == -1) {
      sys.error(s"No release notes found for $version in $displayName")
    }
    val end = lines.indexWhere(line => extractVersion(line).isDefined, start + 1) match {
      case -1 => lines.length
      case end => end
    }

    val text =
      lines
        .slice(start + 1, end)
        .dropWhile(_.isBlank)
        .reverse
        .dropWhile(_.isBlank)
        .reverse
        .mkString("\n")

    if (text.isBlank) {
      sys.error(s"No release notes found for $version in $displayName")
    }

    text
  }

  private def extractVersion(line: String): Option[String] = {
    if (!line.startsWith("## ")) None
    else {
      val heading = line.stripPrefix("## ").trim
      val token = heading.takeWhile(!_.isWhitespace)
      Some(token.stripPrefix("[").stripSuffix("]"))
    }
  }
}
