package org.odenix.mill

import mill.util.Jvm

/** Javadoc helpers for Java modules.
  *
  * This expects JPMS module sources and generates docs via javadoc's
  * --module / --module-source-path mode, not by passing individual source files.
  */
object Javadoc {
  def options(
    baseOptions: Seq[String],
    moduleName: String,
    sourceDirs: Seq[Any],
    modulePath: Seq[Any],
    doclint: Seq[String],
    docTitle: String,
    windowTitle: String,
    bottom: String,
    links: Seq[String],
  ): Seq[String] = {
    val sourcePath = sourceDirs.mkString(java.io.File.pathSeparator)
    val modulePathOptions =
      if (modulePath.isEmpty) Seq.empty
      else Seq("--module-path", modulePath.mkString(java.io.File.pathSeparator))
    val linkOptions = links.flatMap(link => Seq("-link", link))

    baseOptions ++
      Seq(
        "-protected",
        s"-Xdoclint:${doclint.mkString(",")}",
        "-doctitle", docTitle,
        "-windowtitle", windowTitle,
        "-bottom", bottom,
        "--module", moduleName,
        "--module-source-path", s"$moduleName=$sourcePath",
      ) ++
      linkOptions ++
      modulePathOptions
  }

  def generate(
      options: Seq[String],
      outDir: os.Path,
      javaHome: Option[os.Path],
      useArgsFile: Boolean,
      logDebug: String => Unit,
      logInfo: String => Unit
  ): mill.PathRef = {
    val javadocDir = outDir / "javadoc"
    os.makeDir.all(javadocDir)

    val allOptions = options ++ Seq("-d", javadocDir.toString)
    val cmdArgs =
      if (useArgsFile) {
        val argsFile =
          os.temp(
            contents = argsFileContent(allOptions),
            prefix = "javadoc-",
            deleteOnExit = false,
            dir = outDir
          )
        logDebug(s"Creating javadoc options file @$argsFile ...")
        Seq(s"@$argsFile")
      } else {
        allOptions
      }

    logInfo(s"java home: ${javaHome.fold("default")(_.toString)}")
    logInfo("options: " + cmdArgs)

    os.call(
      cmd = Seq(Jvm.jdkTool("javadoc", javaHome)) ++ cmdArgs,
      env = Map(),
      cwd = outDir,
      stdin = os.Inherit,
      stdout = os.Inherit
    )

    mill.PathRef(javadocDir)
  }

  private def argsFileContent(options: Seq[String]): String = {
    options
      .map(s => s"\"${s.replace("\\", "\\\\")}\"")
      .mkString(" ")
  }
}
