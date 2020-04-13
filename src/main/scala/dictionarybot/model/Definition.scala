package dictionarybot.model

case class Definition(
  partOfSpeech: String,
  definition: String,
  example: Option[String],
  imageUrl: Option[String],
  emoji: Option[String]
)
