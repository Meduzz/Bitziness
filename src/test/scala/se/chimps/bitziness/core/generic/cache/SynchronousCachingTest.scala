package se.chimps.bitziness.core.generic.cache

import java.util.concurrent.TimeUnit

import com.google.common.cache.{CacheBuilder, LoadingCache}
import org.scalatest.FunSuite

/**
 * Test of the synchronous caching implementation.
 */
class SynchronousCachingTest extends FunSuite with SynchronousCache[String, Subject] {

  test("LocalCache should store and fetch values successfully") {

    val value1 = new Subject("test")
    val value2 = new Subject("spam")

    withCache(c => {
			c.put("test", value1)
			c.put("spam", value2)
		})

		withCache(c => {
			assert(c.asMap().containsKey("test"))
			assert(c.asMap().containsKey("spam"))
		})

		withCache(c => {
			assert(c.getUnchecked("test").test.equals("test"))
			assert(c.getUnchecked("spam").test.equals("spam"))
		})

		withCache(c => {
			c.invalidate("test")
			assert(!c.asMap().containsKey("test"))

			c.invalidate("spam")
			assert(!c.asMap().containsKey("spam"))
		})

  }

	test("CacheLoader plugs and playes") {

		withCache(c => {
			assert(!c.asMap().containsKey("test"))
			assert(!c.asMap().containsKey("spam"))

			assert(c.getUnchecked("test").test.equals("test"))
			assert(c.getUnchecked("spam").test.equals("spam"))
		})

	}

	override def setupCache(builder:CacheBuilder[String, Subject]):LoadingCache[String, Subject] = {
		builder.expireAfterWrite(10L, TimeUnit.SECONDS).maximumSize(10L).build(CacheLoader(key => Subject(key)))
	}
}

