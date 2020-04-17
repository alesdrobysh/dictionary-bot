package dictionarybot.model

case class DictionaryRecord(
  word: String,
  pronunciation: Option[String],
  definitions: List[Definition]
) {
  def toHtml: String =
    s"""
       |<b>$word</b>
       |${pronunciation.map(p => s"<i>/$p/</i>").getOrElse("")}
       |${definitions.map(_.toHtml).mkString("")}
       |""".stripMargin
}
