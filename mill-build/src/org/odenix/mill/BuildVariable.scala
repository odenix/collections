package org.odenix.mill

/** Reads build variables, which can be set either as environment variables or system properties.
  *
  * Use only inside `Task.Input`. The `env` argument should be `Task.env`, not `sys.env`.
  */
final case class BuildVariable(name: String, aliases: String*) {
  def get(env: Map[String, String]): Option[String] =
    names.iterator.map(value(env, _)).collectFirst { case Some(value) => value }

  def require(env: Map[String, String]): String =
    get(env) match {
      case None =>
        sys.error(s"Env var or system property $displayName is required")
      case Some(value) if value.isBlank =>
        sys.error(s"Env var or system property $displayName must not be blank")
      case Some(value) =>
        value
    }

  private def value(env: Map[String, String], name: String): Option[String] =
    env.get(name).orElse(Option(System.getProperty(name)))

  private def names: Seq[String] =
    name +: aliases

  private def displayName: String =
    names.mkString(" or ")
}
