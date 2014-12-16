package se.chimps.bitziness.core.generic

import java.util.concurrent.atomic.AtomicLong

import se.chimps.bitziness.core.generic.Caching.{Config, Cache, CacheFactory, CacheBase}

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.concurrent.{Promise, Future}
import scala.reflect.ClassTag
import scala.util.Try

/**
 * Some caching abstractions and implementations.
 */
object Caching {

  def apply(base:CacheBase, config:Config):CacheFactory = {
    base(config)
  }

  trait Config {
  }

  trait CacheBase {
    def apply(config:Config):CacheFactory
  }

  trait CacheFactory {
    def load[C]()(implicit evidence:ClassTag[C]):Cache[C]

    // TODO add makeSpace-like method.
  }

  trait Cache[T] {
    def put(key:String, instance:T):Boolean
    def get(key:String):Future[T]
    def has(key:String):Boolean
    def expire(key:String):Boolean
    def expireAll():Boolean
  }
}

object LocalCache extends CacheBase {

  override def apply(config:Config):CacheFactory = new LocalCacheFactory(config.asInstanceOf[LocalConfig])

  private class LocalCacheFactory(val config:LocalConfig) extends CacheFactory {
    lazy val cacheStore = TrieMap[Class[_], LocalCache[_]]()

    override def load[C]()(implicit evidence:ClassTag[C]):Cache[C] = {
      if (!cacheStore.contains(evidence.runtimeClass)) {
        cacheStore.put(evidence.runtimeClass, new LocalCache[C](config))
      }
      cacheStore(evidence.runtimeClass).asInstanceOf[Cache[C]]
    }
  }

  class LocalConfig extends Config {
    val weights = mutable.Map[Class[_], Int]()

    def weight(clazz:Class[_], weight:Int):LocalConfig = {
      weights += (clazz -> weight)
      this
    }
  }

  private class LocalCache[T](val config:LocalConfig)(implicit tag:ClassTag[T]) extends Cache[T] {

    lazy val weight = config.weights.getOrElse(tag.runtimeClass, 1)
    protected lazy val store = TrieMap[String, Item[T]]()

    /*
     * varje typ får en vikt och varje item en timestamp som uppdateras så fort itemen läses.
     */

    override def put(key:String, instance:T):Boolean = {
      store.put(key, Item(instance, new AtomicLong(0L)))
      true
    }

    override def expireAll():Boolean = {
      store.clear()
      store.isEmpty
    }

    override def get(key:String):Future[T] = {
      val p = Promise[T]()
      val item = store(key)

      p.complete(Try(item.value))

      item.vector.incrementAndGet()
      p.future
    }

    override def expire(key:String):Boolean = {
      store.remove(key)
      true
    }

    override def has(key:String):Boolean = store.contains(key)
  }

  case class Item[T](value:T, vector:AtomicLong)
}