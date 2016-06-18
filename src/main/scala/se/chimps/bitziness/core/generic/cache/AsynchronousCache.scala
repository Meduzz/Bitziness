package se.chimps.bitziness.core.generic.cache

import java.util.concurrent.{Callable => Call}

import com.google.common.cache.{CacheBuilder, Cache => AsyncMap}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/**
	*
	*/
trait AsynchronousCache[T,K] {
	def setupCache(builder:CacheBuilder[T,K]):AsyncMap[T,K]

	private val cache:AsyncMap[T,K] = setupCache(CacheBuilder.newBuilder().asInstanceOf[CacheBuilder[T,K]])

	def withCache[U](func:(AsyncMap[T,K]) => U):U = {
		func(cache)
	}

	implicit def explicit(cache:AsyncMap[T,K]):CacheExplicits[T,K] = new CacheExplicits(cache)
}

class CacheExplicits[T,K](cache:AsyncMap[T,K]) {
	def getOption(key:T):Option[K] = Option(cache.getIfPresent(key))
}

/**
	* Asynchronous cacheLoader that should be used in Cache.get
	*/
object AsyncCacheLoader {
	def apply[T,K](key:T, func:(T)=>Future[K])(implicit ec:ExecutionContext, duration: Duration):Call[K] = new Call[K] {
		override def call():K = {
			Await.result(func(key), duration)
		}
	}
}

object Callable {
	def apply[T](func:() => T):Call[T] = new Call[T] {
		override def call():T = {
			func()
		}
	}
}