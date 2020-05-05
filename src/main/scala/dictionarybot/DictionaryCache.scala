package dictionarybot

import cats.effect.Async
import dictionarybot.DictionaryApi.ApiResponse
import dictionarybot.model.DictionaryRecord
import scalacache.{Cache, cachingF}
import scalacache.memcached._
import scalacache.serialization.binary._
import scalacache.Mode

class DictionaryCache[F[_]: Async](cacheAddress: String) {
  type ApiResponseF[V] = ApiResponse[F, V]

  implicit val memcachedCache: Cache[DictionaryRecord] = MemcachedCache(cacheAddress)
  implicit val mode: Mode[ApiResponseF] = scalacache.CatsEffect.modes.async[ApiResponseF]

  def withCache(word: String)(f: ApiResponseF[DictionaryRecord]) =
    cachingF[ApiResponseF, DictionaryRecord](word)(ttl = None)(f)
}
