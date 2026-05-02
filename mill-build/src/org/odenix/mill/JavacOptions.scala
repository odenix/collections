package org.odenix.mill

final case class JavacOptions private (
    release: Option[String] = None,
    proc: Option[String] = None,
    modulePath: Seq[os.Path] = Seq.empty
) {
  def release(value: String): JavacOptions =
    copy(release = Some(value))

  def proc(value: String): JavacOptions =
    copy(proc = Some(value))

  def modulePath(values: Seq[os.Path]): JavacOptions =
    copy(modulePath = modulePath ++ values)

  def ++(other: JavacOptions): JavacOptions =
    JavacOptions(
      release = other.release.orElse(release),
      proc = other.proc.orElse(proc),
      modulePath = (modulePath ++ other.modulePath).distinct
    )

  def render(baseOptions: Seq[String] = Seq.empty): Seq[String] =
    baseOptions ++
      release.toSeq.flatMap(value => Seq("--release", value)) ++
      proc.toSeq.map(value => s"-proc:$value") ++
      modulePathOptions

  private def modulePathOptions: Seq[String] =
    if (modulePath.isEmpty) Seq.empty
    else Seq("--module-path", modulePath.mkString(java.io.File.pathSeparator))
}

object JavacOptions {
  val empty: JavacOptions = JavacOptions()
}
