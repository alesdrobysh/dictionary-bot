package dictionarybot

import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.effect.{Async, ContextShift, Timer}
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.cats.Polling
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.client.Client

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
            _.as[Json]
              .flatMap(json => reply(json.spaces2))
          )
          .void
      }
    }
  }
}
