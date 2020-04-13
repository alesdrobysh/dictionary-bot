package dictionarybot

import cats.effect.{Async, ContextShift, Timer}
import cats.implicits._
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.cats.Polling
import com.bot4s.telegram.methods.ParseMode
import dictionarybot.model._
import Decoders._
import org.http4s._
import org.http4s.client.Client
import org.http4s.circe.CirceEntityDecoder._

class DictionaryBot[F[_]: Async: Timer: ContextShift](
  token: String,
  client: Client[F],
  dictionaryApiKey: String
) extends Bot[F](token)
    with Polling[F]
    with Commands[F] {

  onCommand("/search") { implicit msg =>
    withArgs { args =>
      {
        val request = Request[F](
          method = Method.GET,
          uri = uri"https://owlbot.info/api/v4/dictionary" / args.headOption
            .getOrElse("") +? ("format", "json"),
          headers = Headers.of(Header("Authorization", s"Token $dictionaryApiKey"))
        )

        client.toHttpApp
          .run(request)
          .flatMap(
            _.as[DictionaryRecord]
              .flatMap(record => reply(record.toMarkdown, ParseMode.Markdown.some))
          )
          .void

      }
    }
  }
}
