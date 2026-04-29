package org.odenix.mill

object ReleaseNotes {
  def fromChangelog(changelogPath: os.Path, version: String): String = {
    val lines = os.read.lines(changelogPath)
    val start = lines.indexWhere(line => extractVersion(line).contains(version))
    if (start == -1) {
      sys.error(s"No release notes found for $version in $changelogPath")
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
      sys.error(s"No release notes found for $version in $changelogPath")
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
