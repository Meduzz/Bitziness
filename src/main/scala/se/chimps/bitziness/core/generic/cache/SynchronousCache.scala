package se.chimps.bitziness.core.generic.cache

import com.google.common.cache.{CacheBuilder, CacheLoader => Loader, LoadingCache => SyncMap}

/**
	*
	*/
trait SynchronousCache[T,K] {
	def setupCache(builder:CacheBuilder[T,K]):SyncMap[T,K]

	private val cache:SyncMap[T,K] = setupCache(CacheBuilder.newBuilder().asInstanceOf[CacheBuilder[T,K]])

	def withCache[U](func:(SyncMap[T,K]) => U):U = {
		func(cache)
	}
}

/**
	* Synchronous cacheLoader wrapper scala style that should be used in the CacheBuilder.build(&lt;here&gt;).
	*/
object CacheLoader {
	def apply[T,K](func:(T)=>K):Loader[T,K] = new Loader[T,K] {
		override def load(key:T):K = func(key)
	}
}
