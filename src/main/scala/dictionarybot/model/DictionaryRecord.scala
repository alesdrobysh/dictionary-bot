package dictionarybot.model

case class DictionaryRecord(
  word: String,
  pronunciation: String,
  definitions: List[Definition]
)
