package dictionarybot

import cats.effect.{Async, ContextShift, Timer}
import cats.implicits._
import com.bot4s.telegram.api.declarative.Commands
import com.bot4s.telegram.cats.Polling
import com.bot4s.telegram.methods.ParseMode
import org.http4s.circe.CirceEntityDecoder._

class DictionaryBot[F[_]: Async: Timer: ContextShift](
  token: String,
  api: DictionaryApi[F],
  cache: DictionaryCache[F]
) extends Bot[F](token)
    with Polling[F]
    with Commands[F] {

  import dictionarybot.model._
  import dictionarybot.model.Decoders._
  import DictionaryCache.CacheError
  import DictionaryApi.ApiError

  onCommand("/search") { implicit msg =>
    withArgs { args =>
      args.headOption match {
        case Some(word) =>
          search(word)
            .flatMap(getResponse(_))
            .flatMap(reply(_, ParseMode.Markdown.some))
            .map(_ => ())

        case None => reply("""Usage: "/search word"""").void
      }
    }
  }

  private def search(word: String): F[Either[ApiError, F[DictionaryRecord]]] =
    cache.get(word).flatMap {
      case Right(record) =>
        record.pure[F].asRight[ApiError].pure[F]
      case Left(CacheError.NotCached(word)) =>
        api
          .search(word)
          .map(
            _.map(
              _.as[DictionaryRecord]
                .flatMap { record => cache.set(record.word, record).map(_ => record) }
            )
          )
    }

  private def getResponse(input: Either[ApiError, F[DictionaryRecord]]): F[String] =
    input match {
      case Left(ApiError.NotFound(word)) => s"_${word}_ not found".pure[F]
      case Left(ApiError.UnexpectedError) =>
        "Oops... Something went wrong. Please retry.".pure[F]
      case Right(record) => record.map(_.toMarkdown)
    }
}
