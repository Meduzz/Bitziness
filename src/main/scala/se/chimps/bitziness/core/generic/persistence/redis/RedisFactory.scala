package se.chimps.bitziness.core.generic.persistence.redis

import akka.actor.ActorSystem
import redis.RedisClient
import se.chimps.bitziness.core.generic.Configs

import scala.concurrent.Future

trait RedisFactory extends Configs {
  implicit def system:ActorSystem

  val host = asString("redis.host").getOrElse("localhost")
  val port = asInt("redis.port").getOrElse(6379)
  val auth = asString("redis.auth")
  val db = asInt("redis.db")

  lazy val redis = RedisClient(host = host, port = port, password = auth, db = db)

  def withRedis[T](op:(RedisClient)=>Future[T]):Future[T] = {
    op(redis)
  }
}
