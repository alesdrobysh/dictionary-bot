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

  def search(word: String): F[Either[ApiError, Response[F]]] = {
    val request = Request[F](
      method = Method.GET,
      uri = root / word +? ("format", "json"),
      headers = Headers.of(Header("Authorization", s"Token $apiKey"))
    )

    client.toHttpApp
      .run(request)
      .map { response: Response[F] =>
        response.status match {
          case Status.Ok => Right(response)
          case Status.NotFound => Left(ApiError.NotFound(word))
          case _ => Left(ApiError.UnexpectedError)
        }
      }
  }
}

object DictionaryApi {
  sealed trait ApiError
  object ApiError {
    case class NotFound(word: String) extends ApiError
    case object UnexpectedError extends ApiError
  }
}
