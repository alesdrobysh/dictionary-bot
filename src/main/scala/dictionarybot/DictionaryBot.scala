package dictionarybot

import cats.Applicative
import cats.effect.{Async, ContextShift, Timer}
import cats.implicits._
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.cats.Polling
import com.bot4s.telegram.methods.ParseMode
import dictionarybot.DictionaryApi.ApiStatus
import dictionarybot.model._
import dictionarybot.model.Decoders._
import org.http4s.circe.CirceEntityDecoder._

class DictionaryBot[F[_]: Async: Timer: ContextShift](
  token: String,
  api: DictionaryApi[F]
) extends Bot[F](token)
    with Polling[F]
    with Commands[F] {

  onCommand("/search") { implicit msg =>
    withArgs { args =>
      api
        .search(args.toList)
        .flatMap(_.traverse(handleApiResponse))
        .flatMap(_.traverse(reply(_, ParseMode.Markdown.some)))
        .void
    }
  }

  private def handleApiResponse: ApiStatus[F] => F[String] = {
    {
      case ApiStatus.EmptyRequest() => Applicative[F].pure("Can not handle an empty string")
      case ApiStatus.NotFound(word) => Applicative[F].pure(s"_${word}_ not found")
      case ApiStatus.UnexpectedError() =>
        Applicative[F].pure("Oops... Something went wrong. Please retry.")
      case ApiStatus.Success(response) => {
        response.as[DictionaryRecord].map(_.toMarkdown)
      }
    }
  }
}
