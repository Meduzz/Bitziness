package se.chimps.bitziness.core.endpoints.redis

import akka.actor.{ActorLogging, ActorRefFactory}
import redis.{RedisClient, RedisPubSub}
import redis.api.pubsub.{PMessage, Message}
import se.chimps.bitziness.core.Endpoint

/**
 * Base trait for Redis pub/sub.
 */
trait RedisPubSubTrait extends Endpoint with ActorLogging {

  implicit private val actorFactory = context

  def onMessage(msg: Message): Unit = {
    log.debug("Received message: {}", msg)
    self ! msg
  }

  def onPMessage(msg:PMessage):Unit = {
    log.debug("Received pmessage: {}", msg)
    self ! msg
  }

  protected def setupRedisPubSub(builder:RedisBuilder):Tuple2[RedisClient, RedisPubSub]

  protected val (redis:RedisClient, pubsub:RedisPubSub) = setupRedisPubSub(new RedisBuilderImpl(this))
}

trait RedisBuilder {
  def host(host:String):RedisBuilder
  def port(port:Int):RedisBuilder
  def subscribeChannel(channels:String*):RedisBuilder
  def subscribePattern(pattern:String*):RedisBuilder
  def messageHandler(handler:(Message)=>Unit):RedisBuilder
  def pMessageHandler(handler:(PMessage)=>Unit):RedisBuilder
  def password(pwd:String):RedisBuilder
  def build():Tuple2[RedisClient, RedisPubSub]
}

private class RedisBuilderImpl(endpoint:RedisPubSubTrait)(implicit val actorFactory:ActorRefFactory) extends RedisBuilder {
  private var host:String = "localhost"
  private var port:Int = 6379
  private var password:Option[String] = None
  private var channels:Seq[String] = Seq()
  private var patterns:Seq[String] = Seq()
  private var msgHandler:(Message)=>Unit = endpoint.onMessage
  private var pmsgHandler:(PMessage)=>Unit = endpoint.onPMessage

  override def host(host: String): RedisBuilder = {
    this.host = host
    this
  }

  override def subscribePattern(pattern: String*): RedisBuilder = {
    this.patterns = pattern
    this
  }

  override def messageHandler(handler: (Message) => Unit): RedisBuilder = {
    this.msgHandler = handler
    this
  }

  override def password(pwd: String): RedisBuilder = {
    this.password = Some(pwd)
    this
  }

  override def pMessageHandler(handler: (PMessage) => Unit): RedisBuilder = {
    this.pmsgHandler = handler
    this
  }

  override def port(port: Int): RedisBuilder = {
    this.port = port
    this
  }

  override def build():Tuple2[RedisClient, RedisPubSub] = (new RedisClient(host, port, password), new RedisPubSub(host, port, channels, patterns, msgHandler, pmsgHandler, password))

  override def subscribeChannel(channels: String*): RedisBuilder = {
    this.channels = channels
    this
  }
}