package dictionarybot.model

import cats.implicits._

case class Definition(
  partOfSpeech: String,
  definition: String,
  example: Option[String],
  imageUrl: Option[String],
  emoji: Option[String]
) {
  def toHtml: String =
    s"""
       |<i>${partOfSpeech}</i>
       |$definition ${emoji.getOrElse("")}
       |${exampleHtmlOption.getOrElse("")}
       |""".stripMargin

  private def exampleHtmlOption: Option[String] = example match {
    case Some(value) =>
      s"""
         |
         |<i>"$value"</i>
         |""".some
    case None => None
  }
}
