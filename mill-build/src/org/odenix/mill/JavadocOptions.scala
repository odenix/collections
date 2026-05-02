package org.odenix.mill

final case class JavadocOptions private (
    protectedMembers: Option[Boolean] = None,
    xdoclint: Option[Seq[String]] = None,
    title: Option[String] = None,
    bottom: Option[String] = None,
    links: Seq[String] = Seq.empty,
    module: Option[String] = None,
    sourceDirs: Seq[os.Path] = Seq.empty,
    modulePath: Seq[os.Path] = Seq.empty
) {
  def protectedMembers(value: Boolean = true): JavadocOptions =
    copy(protectedMembers = Some(value))

  def xdoclint(values: String*): JavadocOptions =
    copy(xdoclint = Some(values))

  def title(value: String): JavadocOptions =
    copy(title = Some(value))

  def bottom(value: String): JavadocOptions =
    copy(bottom = Some(value))

  def links(values: String*): JavadocOptions =
    copy(links = links ++ values)

  def module(value: String): JavadocOptions =
    copy(module = Some(value))

  def sourceDirs(values: Seq[os.Path]): JavadocOptions =
    copy(sourceDirs = sourceDirs ++ values)

  def modulePath(values: Seq[os.Path]): JavadocOptions =
    copy(modulePath = modulePath ++ values)

  def ++(other: JavadocOptions): JavadocOptions =
    JavadocOptions(
      protectedMembers = other.protectedMembers.orElse(protectedMembers),
      xdoclint = other.xdoclint.orElse(xdoclint),
      title = other.title.orElse(title),
      bottom = other.bottom.orElse(bottom),
      links = (links ++ other.links).distinct,
      module = other.module.orElse(module),
      sourceDirs = (sourceDirs ++ other.sourceDirs).distinct,
      modulePath = (modulePath ++ other.modulePath).distinct
    )

  def render(baseOptions: Seq[String] = Seq.empty): Seq[String] =
    baseOptions ++
      protectedMembers.filter(identity).toSeq.flatMap(_ => Seq("-protected")) ++
      xdoclint.toSeq.flatMap(values => Seq(s"-Xdoclint:${values.mkString(",")}")) ++
      title.toSeq.flatMap(value => Seq("-doctitle", value, "-windowtitle", value)) ++
      bottom.toSeq.flatMap(value => Seq("-bottom", value)) ++
      links.distinct.flatMap(value => Seq("-link", value)) ++
      module.toSeq.flatMap(value => Seq("--module", value)) ++
      moduleSourcePathOptions ++
      modulePathOptions

  private def moduleSourcePathOptions: Seq[String] =
    module match {
      case Some(moduleName) if sourceDirs.nonEmpty =>
        val sourcePath = sourceDirs.mkString(java.io.File.pathSeparator)
        Seq("--module-source-path", s"$moduleName=$sourcePath")
      case _ =>
        Seq.empty
    }

  private def modulePathOptions: Seq[String] =
    if (modulePath.isEmpty) Seq.empty
    else Seq("--module-path", modulePath.mkString(java.io.File.pathSeparator))
}

object JavadocOptions {
  val empty: JavadocOptions = JavadocOptions()
}
