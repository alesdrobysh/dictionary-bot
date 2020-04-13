package dictionarybot.model

import cats.implicits._

case class Definition(
  partOfSpeech: String,
  definition: String,
  example: Option[String],
  imageUrl: Option[String],
  emoji: Option[String]
) {
  def toMarkdown: String =
    s"""
       |_${partOfSpeech}_
       |$definition ${emoji.getOrElse("")}
       |${exampleMarkdownOption.getOrElse("")}
       |""".stripMargin

  private def exampleMarkdownOption: Option[String] = example match {
    case Some(value) =>
      s"""
         |
         |_"$value"_
         |""".some
    case None => None
  }
}
