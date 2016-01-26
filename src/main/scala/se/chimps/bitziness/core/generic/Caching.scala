package se.chimps.bitziness.core.generic

import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.util.ByteString
import redis.RedisClient
import se.chimps.bitziness.core.generic.Caching.Cache
import se.chimps.bitziness.core.generic.Serializers.JSONSerializer

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.reflect.ClassTag
import scala.util.{Success, Failure, Try}

/**
 * Some caching abstractions and implementations.
 */
object Caching {
  trait Cache[T] {
    def put(key:String, instance:T):Future[Boolean]
    def get(key:String):Future[T]
    def has(key:String):Future[Boolean]
    def expire(key:String):Future[Boolean]
    def expireAll():Future[Boolean]
  }
}

object LocalCache {

  def apply(config:LocalConfig):LocalCacheFactory = new LocalCacheFactory(config)

  class LocalCacheFactory(val config:LocalConfig) {
    lazy val cacheStore = TrieMap[Class[_], Cache[_]]()

    def load[C]()(implicit evidence:ClassTag[C], ec:ExecutionContext):Cache[C] = {
      if (!cacheStore.contains(evidence.runtimeClass)) {
        cacheStore.put(evidence.runtimeClass, new LocalCache[C](config))
      }
      cacheStore(evidence.runtimeClass).asInstanceOf[Cache[C]]
    }
  }

  class LocalConfig {
    val weights = mutable.Map[Class[_], Int]()

    def weight(clazz:Class[_], weight:Int):LocalConfig = {
      weights += (clazz -> weight)
      this
    }
  }

  private class LocalCache[T](val config:LocalConfig)(implicit tag:ClassTag[T], ec:ExecutionContext) extends Cache[T] {

    lazy val weight = config.weights.getOrElse(tag.runtimeClass, 1)
    protected lazy val store = TrieMap[String, Item[T]]()

    /*
     * varje typ får en vikt och varje item en timestamp som uppdateras så fort itemen läses.
     */

    override def put(key:String, instance:T):Future[Boolean] = {
      store.put(key, Item(instance, new AtomicLong(0L)))
      Future {true}
    }

    override def expireAll():Future[Boolean] = {
      Future {store.clear(); store.isEmpty}
    }

    override def get(key:String):Future[T] = {
      val p = Promise[T]()
      val item = store(key)

      p.complete(Try(item.value))

      item.vector.incrementAndGet()
      p.future
    }

    override def expire(key:String):Future[Boolean] = {
      store.remove(key)
      Future {true}
    }

    override def has(key:String):Future[Boolean] = Future {store.contains(key)}
  }

  case class Item[T](value:T, vector:AtomicLong)
}

object RedisCache {

  def apply(config: RedisConfig)(implicit system:ActorSystem): RedisCacheFactory = new RedisCacheFactory(config)

  case class RedisConfig(host:String = "localhost"
                         , port:Int = 6379
                         , auth:Option[String] = None
                         , db:Option[Int] = None
                         , expireSec:Option[Int] = None
                         , maxKeys:Option[Int] = None)

  class RedisCacheFactory(val config:RedisConfig)(implicit val system:ActorSystem) {
    def load[C]()(implicit evidence: Manifest[C], ec:ExecutionContext): Cache[C] = new RedisCache[C](config.expireSec, config.maxKeys, RedisClient(config.host, config.port, config.auth, config.db))
  }

  class RedisCache[C](val expire:Option[Int], val maxKeys:Option[Int], val client:RedisClient)(implicit val evidence:Manifest[C], ec:ExecutionContext) extends Cache[C] with JSONSerializer {

    val prefix = (key:String)=>s"cache.$key"
    val keysSet = "cachedkeys"

    override def put(key: String, instance: C): Future[Boolean] = expire match {
      case Some(time) => client.setex(prefix(key), time.toLong, toJSON(instance.asInstanceOf[AnyRef]))
        .map {
          case true => sadd(key)
          case false => false
        }
      case None => client.set(prefix(key), toJSON(instance.asInstanceOf[AnyRef]))
        .map {
          case true => sadd(key)
          case false => false
        }
    }

    override def expireAll(): Future[Boolean] = {
      Future {
        import Waitable._
        client.smembers(keysSet).get[Seq[ByteString]].map(_.utf8String).foreach { key =>
          client.del(key)
          client.srem(keysSet, key)
        }
        true
      }
    }

    override def get(key: String): Future[C] = {
      client.get(prefix(key)).map[C] {
        case Some(bytes) => updateExpire(key); fromJSON[C](bytes.utf8String)
        case None => throw new RuntimeException(s"No value found for key: $key.")
      }
    }

    override def expire(key: String): Future[Boolean] = {
      import Waitable._
      Future {client.srem(keysSet, key); client.del(prefix(key)).get[Long] == 1}
    }

    override def has(key: String): Future[Boolean] = {
      client.sismember(keysSet, key)
    }

    private def sadd(key:String):Boolean = {
      val i = client.sadd(keysSet, key)

      while (!i.isCompleted) {}

      i.value match {
        case Some(Success(l)) => l <= 1L
        case Some(Failure(e)) => false
        case None => false
      }
    }

    private def updateExpire(key:String) = {
      expire match {
        case Some(time) => client.expire(prefix(key), time)
        case None => Unit
      }
    }
  }
}