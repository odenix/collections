package org.odenix.mill

import mill.util.Jvm

/** Javadoc helpers for Java modules.
  *
  * This expects JPMS module sources and generates docs via javadoc's
  * --module / --module-source-path mode, not by passing individual source files.
  */
object Javadoc {
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
