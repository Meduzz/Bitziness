package se.chimps.bitziness.core.service.plugins.amqp

import akka.actor.{Actor, ActorRef}
import com.rabbitmq.client.AMQP.BasicProperties
import io.github.drexin.akka.amqp.AMQP
import io.github.drexin.akka.amqp.AMQP._
import se.chimps.bitziness.core.service.Service
import se.chimps.bitziness.core.service.plugins.Plugin
import akka.io.IO
import scala.concurrent.duration._

/**
 * Base trait for consuming and producing AMQP messages.
 */
trait Amqp extends Plugin { service:Service =>

  private val endpoint = IO(AMQP)
  private val amqpSettings:AmqpSettings = setupAmqpEndpoint(new AmqpBuilderImpl(endpoint))

  def amqp:Receive = {
    case Connected|"setup" => setup()
  }

  protected def subscribe(queueName:String, autoack:Boolean = true, handler:(Delivery)=>Unit) = {
    // TODO we need a new actor that simply receives Delivery and runs it through the handler.
  }

  protected def publish[T](exchange:String, routingKey:String, body:T, mandatory:Boolean = false, immidiate:Boolean = false, props:Option[BasicProperties] = None)(implicit converter:(T)=>Array[Byte]) = {
    endpoint ! Publish(exchange, routingKey, converter(body), mandatory, immidiate, props)
  }

  // TODO add support to remove exchanges and unbind queues etc.

  protected def setupAmqpEndpoint(builder:AmqpBuilder):AmqpSettings

  override def receive: Actor.Receive = amqp.orElse(service.receive)

  private def setup() {
    if (amqpSettings != null) {
      amqpSettings.exchanges.foreach { msg =>
        endpoint ! msg
      }

      amqpSettings.queues.foreach { msg =>
        endpoint ! msg
      }

      amqpSettings.binds.foreach { msg =>
        endpoint ! msg
      }
    } else {
      import service.context.dispatcher
      service.context.system.scheduler.scheduleOnce(1.second, service.self, "setup")
    }
  }
}

trait AmqpBuilder {
  def connect(uri:String):AmqpBuilder
  def declareExchange(name:String, exchangeType:String = ExchangeTypes.DIRECT, durable:Boolean = true, autodelete:Boolean = false, internal:Boolean = false, props:Map[String, AnyRef] = Map()):AmqpBuilder
  def declareQueue(name:String, durable:Boolean = true, exclusive:Boolean = false, autodelete:Boolean = false, props:Map[String, AnyRef] = Map()):AmqpBuilder
  def bind(exchange:String, queue:String, routingKey:String, props:Map[String, AnyRef] = Map()):AmqpBuilder
  def build():AmqpSettings
}

private class AmqpBuilderImpl(val endpoint:ActorRef) extends AmqpBuilder {
  private var uri:String = _
  private var exchanges:Seq[DeclareExchange] = Seq()
  private var queues:Seq[DeclareQueue] = Seq()
  private var binds:Seq[BindQueue] = Seq()

  override def connect(uri: String): AmqpBuilder = {
    this.uri = uri
    this
  }

  override def declareExchange(name: String, exchangeType: String, durable: Boolean, autodelete: Boolean, internal: Boolean, props: Map[String, AnyRef]): AmqpBuilder = {
    exchanges = exchanges ++ Seq(new DeclareExchange(name, exchangeType, durable, autodelete, internal, props))
    this
  }

  override def declareQueue(name: String, durable: Boolean, exclusive: Boolean, autodelete: Boolean, props: Map[String, AnyRef]): AmqpBuilder = {
    queues = queues ++ Seq(new DeclareQueue(name, durable, exclusive, autodelete, props))
    this
  }

  override def bind(exchange: String, queue: String, routingKey: String, props: Map[String, AnyRef]): AmqpBuilder = {
    binds = binds ++ Seq(new BindQueue(exchange, queue, routingKey, props))
    this
  }

  override def build(): AmqpSettings = {
    endpoint ! Connect(this.uri)
    new AmqpSettings(exchanges, queues, binds)
  }
}

case class AmqpSettings(exchanges:Seq[Command], queues:Seq[Command], binds:Seq[Command])

object ExchangeTypes {
  val TOPIC = "topic"
  val FANOUT = "fanout"
  val DIRECT = "direct"
  val HEADERS = "headers"
}
