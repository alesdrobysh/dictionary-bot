package dictionarybot.model

case class DictionaryRecord(
  word: String,
  pronunciation: String,
  definitions: List[Definition]
) {
  def toMarkdown: String =
    s"""
       |*$word*
       |_/$pronunciation/_
       |${definitions.map(_.toMarkdown).mkString("")}
       |""".stripMargin
}
