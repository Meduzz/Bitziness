package se.chimps.bitziness.core.generic

import org.scalatest.FunSuite
import se.chimps.bitziness.core.generic.Caching.Cache
import se.chimps.bitziness.core.generic.LocalCache.LocalConfig

import scala.reflect._

/**
 * Test of the provided caching implementations.
 */
class CachingTest extends FunSuite {

  def localCache[T](c:LocalConfig)(implicit tag:ClassTag[T]):Cache[T] = {
    Caching(LocalCache, c).load[T]()
  }

  test("LocalCache should store and fetch values successfully") {
    val cache = localCache[Subject](new LocalConfig().weight(classOf[Subject], 10))

    val value1 = new Subject("test")
    val value2 = new Subject("spam")

    assert(cache.put("test", value1))
    assert(cache.put("spam", value2))

    assert(cache.has("test"))
    assert(cache.has("spam"))

    val future1 = cache.get("test")
    val future2 = cache.get("spam")

    import scala.concurrent.ExecutionContext.Implicits.global

    for {
      v1 <- future1
    } yield assert(v1.test.equals("test"))

    for {
      v2 <- future2
    } yield assert(v2.test.equals("spam"))
  }
}

case class Subject(test:String)