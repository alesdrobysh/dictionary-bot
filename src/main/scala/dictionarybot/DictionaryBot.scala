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
      args.headOption
        .map(handleRequest)
        .getOrElse(handleEmptyRequest)
        .flatMap(reply(_, ParseMode.Markdown.some))
        .void
    }
  }

  def handleRequest(word: String): F[String] =
    for {
      searchResult <- search(word)
      response = searchResult2response(searchResult)
    } yield response

  def handleEmptyRequest: F[String] = """Usage: "/search word"""".pure[F]

  private def search(word: String): F[Either[ApiError, DictionaryRecord]] =
    cache.get(word).flatMap {
      case Right(record) =>
        record.asRight[ApiError].pure[F]
      case Left(CacheError.NotCached(word)) =>
        api
          .search(word)
          .flatMap(
            _.traverse(
              _.as[DictionaryRecord]
                .flatMap { record => cache.set(record.word, record).map(_ => record) }
            )
          )
    }

  private val searchResult2response: Either[ApiError, DictionaryRecord] => String = {
    case Left(ApiError.NotFound(word)) => s"_${word}_ not found"
    case Left(ApiError.UnexpectedError) => "Oops... Something went wrong. Please retry."
    case Right(record) => record.toMarkdown
  }
}
