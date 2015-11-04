package se.chimps.bitziness.core.endpoints.redis

import akka.actor.{ActorLogging, ActorRefFactory}
import redis.RedisPubSub
import redis.api.pubsub.{Message, PMessage}
import se.chimps.bitziness.core.generic.persistence.redis.endpoint.RedisEndpoint

/**
 * Base trait for Redis pub/sub.
 */
trait RedisPubSubEndpoint extends RedisEndpoint with ActorLogging {

  implicit val system = context.system

  def onMessage(msg: Message): Unit = {
    log.info("Received message: {}", msg)
    self ! msg
  }

  def onPMessage(msg:PMessage):Unit = {
    log.info("Received pmessage: {}", msg)
    self ! msg
  }

  protected def setupRedisPubSub(builder:RedisBuilder):RedisPubSub
  protected val pubsub:RedisPubSub = setupRedisPubSub(new RedisBuilderImpl(this, host, port, auth))
}

trait RedisBuilder {
  def subscribeChannel(channels:String*):RedisBuilder
  def subscribePattern(pattern:String*):RedisBuilder
  def messageHandler(handler:(Message)=>Unit):RedisBuilder
  def pMessageHandler(handler:(PMessage)=>Unit):RedisBuilder
  def build():RedisPubSub
}

private class RedisBuilderImpl(endpoint:RedisPubSubEndpoint, host:String, port:Int, password:Option[String])(implicit val actorFactory:ActorRefFactory) extends RedisBuilder {
  private var channels:Seq[String] = Seq()
  private var patterns:Seq[String] = Seq()
  private var msgHandler:(Message)=>Unit = endpoint.onMessage
  private var pmsgHandler:(PMessage)=>Unit = endpoint.onPMessage

  override def subscribePattern(pattern: String*): RedisBuilder = {
    this.patterns = pattern
    this
  }

  override def messageHandler(handler: (Message) => Unit): RedisBuilder = {
    this.msgHandler = handler
    this
  }

  override def pMessageHandler(handler: (PMessage) => Unit): RedisBuilder = {
    this.pmsgHandler = handler
    this
  }

  override def build():RedisPubSub = new RedisPubSub(host, port, channels, patterns, msgHandler, pmsgHandler, password)

  override def subscribeChannel(channels: String*): RedisBuilder = {
    this.channels = channels
    this
  }
}