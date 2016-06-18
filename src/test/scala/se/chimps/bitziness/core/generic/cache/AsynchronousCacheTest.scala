package se.chimps.bitziness.core.generic.cache

import java.util.UUID
import java.util.concurrent.TimeUnit

import com.google.common.cache.{Cache, CacheBuilder}
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
	* Created by meduzz on 18/06/16.
	*/
class AsynchronousCacheTest extends FunSuite with AsynchronousCache[String, Subject] with ScalaFutures {
	override def setupCache(builder:CacheBuilder[String, Subject]):Cache[String, Subject] = builder.maximumSize(10).build()

	test("empty keys are empty") {

		withCache(c => {
			assert(c.getIfPresent("spam") == null)
			c.getOption("spam") match {
				case Some(subject) => fail("Cache had value for key spam.")
				case None =>
			}
		})

	}

	test("explicit are working") {

		withCache(c => {
			c.put("test", new Subject("test"))

			c.getOption("spam") match {
				case Some(subject) => fail("Cache had value for key spam.")
				case None =>
			}

			c.getOption("test") match {
				case Some(subject) => assert(subject.test.equals("test"))
				case None => fail("Cache did not have value for key test.")
			}
		})

	}

	test("async bits are not completely broken") {
		import scala.concurrent.ExecutionContext.Implicits.global
		implicit val duration = Duration(3L, TimeUnit.SECONDS)

		val subject = withCache(c => {
			c.get("asdf", AsyncCacheLoader[String, Subject]("asdf", (key) => Future(Subject(key))))
		})

		assert(subject != null)
		assert(subject.test.equals("asdf"))

		val uuid = withCache(c => {
			c.get("1", Callable(() => Subject(UUID.randomUUID().toString)))
		})

		assert(uuid != null)
		assert(uuid.test != null)
	}
}
