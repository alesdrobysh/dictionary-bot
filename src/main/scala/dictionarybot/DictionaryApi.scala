package dictionarybot

import cats.Applicative
import cats.implicits._
import org.http4s._
import org.http4s.client.Client

class DictionaryApi[F[_]: Applicative](
  client: Client[F],
  apiKey: String
) {
  val root = uri"https://owlbot.info/api/v4/dictionary"

  import dictionarybot.DictionaryApi._

  def search(word: String): F[ApiStatus[F]] = {
    val request = Request[F](
      method = Method.GET,
      uri = root / word +? ("format", "json"),
      headers = Headers.of(Header("Authorization", s"Token $apiKey"))
    )

    client.toHttpApp
      .run(request)
      .map { response: Response[F] =>
        response.status match {
          case Status.Ok => ApiStatus.Success(response)
          case Status.NotFound => ApiStatus.NotFound(word)
          case _ => ApiStatus.UnexpectedError()
        }
      }
  }
}

object DictionaryApi {
  sealed trait ApiStatus[F[_]]
  object ApiStatus {
    case class NotFound[F[_]](word: String) extends ApiStatus[F]
    case class UnexpectedError[F[_]]() extends ApiStatus[F]
    case class Success[F[_]](response: Response[F]) extends ApiStatus[F]
  }
}