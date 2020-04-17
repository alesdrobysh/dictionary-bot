package dictionarybot

import cats.effect.Async
import cats.implicits._
import dictionarybot.model.DictionaryRecord
import scalacache.{Cache, get => getCache, put => putCache}
import scalacache.memcached._
import scalacache.serialization.binary._
import scalacache.Mode

class DictionaryCache[F[_]: Async](cacheAddress: String) {
  import DictionaryCache._

  implicit val memcachedCache: Cache[DictionaryRecord] = MemcachedCache(cacheAddress)
  implicit val mode: Mode[F] = scalacache.CatsEffect.modes.async[F]

  def set(word: String, record: DictionaryRecord): F[Any] = putCache(word)(record)

  def get(word: String): F[Either[CacheError, DictionaryRecord]] =
    getCache(word).map {
      _.toRight[CacheError](CacheError.NotCached(word))
    }
}

object DictionaryCache {
  sealed trait CacheError
  object CacheError {
    case class NotCached(word: String) extends CacheError
  }
}
