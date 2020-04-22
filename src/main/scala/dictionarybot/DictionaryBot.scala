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
) extends AbstractBot[F](token)
    with Polling[F]
    with Commands[F] {

  import dictionarybot.model._
  import dictionarybot.model.Decoders._
  import DictionaryApi._

  onCommand("/search") { implicit msg =>
    withArgs { args =>
      args.headOption
        .map(handleRequest)
        .getOrElse(handleEmptyRequest)
        .onError({ case error => logger.error(error.toString).pure[F] })
        .flatMap(reply(_, ParseMode.HTML.some))
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
    cache
      .withCache(word)(searchApi(word))
      .value

  private def searchApi(word: String): ApiResponse[F, DictionaryRecord] =
    api
      .search(word)
      .flatMap(_.attemptAs[DictionaryRecord].leftMap(decodeFailure => {
        logger.error(decodeFailure.toString)
        ApiError.UnexpectedError.asInstanceOf[ApiError]
      }))

  private val searchResult2response: Either[ApiError, DictionaryRecord] => String = {
    case Left(ApiError.NotFound(word)) => s"<i>$word</i> not found"
    case Left(ApiError.UnexpectedError) => "Oops... Something went wrong. Please retry."
    case Right(record) => record.toHtml
  }
}
