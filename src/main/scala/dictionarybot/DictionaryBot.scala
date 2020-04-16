package dictionarybot

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
  api: DictionaryApi[F],
  cache: DictionaryCache[F]
) extends Bot[F](token)
    with Polling[F]
    with Commands[F] {

  import DictionaryBot._

  onCommand("/search") { implicit msg =>
    withArgs { args =>
      processArgs(args.toList)
        .flatMap(botStatus2response)
        .flatMap(reply(_, ParseMode.Markdown.some))
        .void
    }
  }

  private def processArgs(args: List[String]): F[BotStatus[F]] =
    args.headOption match {
      case Some(word) =>
        cache.get(word).flatMap {
          case Some(record) =>
            BotStatus
              .Success(record.pure[F])
              .asInstanceOf[BotStatus[F]]
              .pure[F]

          case None =>
            api
              .search(word)
              .map(apiStatus2botStatus)
              .flatMap {
                case BotStatus.Success(frecord) => {
                  frecord
                    .flatMap { record => cache.set(record.word, record) }
                    .map(_ => BotStatus.Success(frecord).asInstanceOf[BotStatus[F]])
                }
                case x => x.pure[F]
              }
        }
      case None => BotStatus.EmptyRequest().asInstanceOf[BotStatus[F]].pure[F]
    }

  private def apiStatus2botStatus(apiStatus: ApiStatus[F]): BotStatus[F] = apiStatus match {
    case ApiStatus.NotFound(word) => BotStatus.NotFound(word)
    case ApiStatus.UnexpectedError() => BotStatus.UnexpectedError()
    case ApiStatus.Success(response) => BotStatus.Success(response.as[DictionaryRecord])
  }

  private def botStatus2response(botStatus: BotStatus[F]): F[String] = botStatus match {
    case BotStatus.EmptyRequest() => "Enter a word to search".pure[F]
    case BotStatus.UnexpectedError() => "Oops... Something went wrong. Please retry.".pure[F]
    case BotStatus.NotFound(word) => s"_${word}_ not found".pure[F]
    case BotStatus.Success(record) => record.map(_.toMarkdown)
  }
}

object DictionaryBot {
  sealed trait BotStatus[F[_]]
  object BotStatus {
    case class EmptyRequest[F[_]]() extends BotStatus[F]
    case class UnexpectedError[F[_]]() extends BotStatus[F]
    case class NotFound[F[_]](word: String) extends BotStatus[F]
    case class Success[F[_]](record: F[DictionaryRecord]) extends BotStatus[F]
  }
}
