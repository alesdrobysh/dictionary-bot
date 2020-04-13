package dictionarybot.model

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

object Decoders {
  implicit val definitionDecoder: Decoder[Definition] =
    Decoder.forProduct5("type", "definition", "example", "image_url", "emoji")(
      Definition.apply
    )
  implicit val recordDecoder: Decoder[DictionaryRecord] = deriveDecoder[DictionaryRecord]
}
