package dictionarybot.model

case class DictionaryRecord(
  word: String,
  pronunciation: Option[String],
  definitions: List[Definition]
) {
  def toMarkdown: String =
    s"""
       |*$word*
       |${pronunciation.map(p => s"_/$p/_").getOrElse("")}
       |${definitions.map(_.toMarkdown).mkString("")}
       |""".stripMargin
}
