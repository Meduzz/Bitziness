package se.chimps.bitziness.core.generic

import akka.actor.ActorSystem
import org.scalatest.FunSuite
import redis.RedisClient
import se.chimps.bitziness.core.generic.Caching.Cache
import se.chimps.bitziness.core.generic.LocalCache.LocalConfig
import se.chimps.bitziness.core.generic.RedisCache.RedisConfig

import scala.reflect._
import Waitable._
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Test of the provided caching implementations.
 */
class CachingTest extends FunSuite {

  def localCache[T](c:LocalConfig)(implicit tag:ClassTag[T]):Cache[T] = {
    LocalCache(c).load[T]()
  }

  def redisCache[T](c:RedisConfig)(implicit evidence:Manifest[T], system:ActorSystem):Cache[T] = {
    RedisCache(c).load[T]()
  }

  test("LocalCache should store and fetch values successfully") {
    val cache = localCache[Subject](new LocalConfig().weight(classOf[Subject], 10))

    val value1 = new Subject("test")
    val value2 = new Subject("spam")

    assert(cache.put("test", value1).get[Boolean])
    assert(cache.put("spam", value2).get[Boolean])

    assert(cache.has("test").get[Boolean])
    assert(cache.has("spam").get[Boolean])

    val future1 = cache.get("test")
    val future2 = cache.get("spam")

    for {
      v1 <- future1
    } yield assert(v1.test.equals("test"))

    for {
      v2 <- future2
    } yield assert(v2.test.equals("spam"))

    assert(cache.expire("test").get[Boolean])
    assert(cache.expire("spam").get[Boolean])

    assert(!cache.has("test").get[Boolean])
    assert(!cache.has("spam").get[Boolean])
  }

  test("RedisCache does the basics") {
    implicit val system = ActorSystem()
    val cache = redisCache[Subject](RedisConfig())

    val value1 = new Subject("test")
    val value2 = new Subject("spam")

    assert(cache.put("test", value1).get[Boolean])
    assert(cache.put("spam", value2).get[Boolean])

    assert(cache.has("test").get[Boolean])
    assert(cache.has("spam").get[Boolean])

    val future1 = cache.get("test")
    val future2 = cache.get("spam")

    for {
      v1 <- future1
    } yield assert(v1.test.equals("test"))

    for {
      v2 <- future2
    } yield assert(v2.test.equals("spam"))

    assert(cache.expire("test").get[Boolean])
    assert(cache.expire("spam").get[Boolean])

    assert(!cache.has("test").get[Boolean])
    assert(!cache.has("spam").get[Boolean])

    system.shutdown()
  }

  test("RedisCache does some advanced features") {
    implicit val system = ActorSystem()
    val cache = redisCache[Subject](RedisConfig(expireSec = Some(10)))
    val redis = RedisClient()

    val value1 = new Subject("test")
    val value2 = new Subject("spam")

    assert(cache.put("advanced.test", value1).get[Boolean])
    assert(cache.put("advanced.spam", value2).get[Boolean])

    assert(redis.ttl("cache.advanced.test").get[Long] == 10)
    assert(redis.ttl("cache.advanced.spam").get[Long] == 10)

    Thread.sleep(1000)

    assert(cache.get("advanced.test").get[Subject] != null)

    assert(redis.ttl("cache.advanced.test").get[Long] == 10)
    assert(redis.ttl("cache.advanced.spam").get[Long] == 9)

    assert(cache.expireAll().get[Boolean])

    assert(!cache.has("advanced.test").get[Boolean])
    assert(!cache.has("advanced.spam").get[Boolean])

    system.shutdown()
  }
}

case class Subject(test:String)