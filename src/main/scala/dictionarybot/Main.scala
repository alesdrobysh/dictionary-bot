package dictionarybot

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.client.blaze.BlazeClientBuilder
import scala.concurrent.ExecutionContext.global

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    val botToken = System.getenv("BOT_TOKEN")
    val dictionaryApiKey = System.getenv("DICTIONARY_API_KEY")

    if (botToken != null && dictionaryApiKey != null) {
      BlazeClientBuilder[IO](global).resource
        .use { client =>
          {
            val api = new DictionaryApi[IO](client, dictionaryApiKey)
            new DictionaryBot[IO](botToken, api).startPolling.attempt
              .map(println(_))
          }
        }
        .map(_ => ExitCode.Success)
    } else IO.raiseError(new Exception("No bot token or dictionary api key"))
  }
}
