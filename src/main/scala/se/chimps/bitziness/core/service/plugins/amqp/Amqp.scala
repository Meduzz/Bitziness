package se.chimps.bitziness.core.service.plugins.amqp

import akka.actor.ActorRef
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.Envelope
import io.github.drexin.akka.amqp.AMQP
import io.github.drexin.akka.amqp.AMQP._
import se.chimps.bitziness.core.service.Service
import se.chimps.bitziness.core.service.plugins.Plugin
import akka.io.IO
import akka.pattern.ask

import scala.concurrent.Await

/**
 * Base trait for consuming and producing AMQP messages.
 */
trait Amqp extends Plugin { service:Service =>

  private val amqpExtension = IO(AMQP)(service.context.system)
  protected val (amqpConnection:ActorRef, amqpSetting:AmqpSettings):Tuple2[ActorRef, AmqpSettings] = SetupAmqpEndpoint(setupAmqpEndpoint)(new AmqpBuilderImpl(amqpExtension), settings => {
    import scala.concurrent.duration._
    import akka.util.Timeout

    val deluxe = 5.seconds

    implicit val timeout:Timeout = Timeout(deluxe)

    val Connected(_, con:ActorRef) = Await.result((amqpExtension ? Connect(settings.uri)).mapTo[Connected], deluxe)

    con
  })

  private def amqp:Receive = {
    case ExchangeDeclared => amqpSetting.queues.foreach(cmd => amqpConnection ! cmd)
    case QueueDeclared => amqpSetting.binds.foreach(cmd => amqpConnection ! cmd)
    case QueueBound => amqpSetting.subscribes.foreach(cmd => amqpConnection ! cmd)
    case Connected => amqpSetting.exchanges.foreach(cmd => amqpConnection ! cmd)
    case Delivery(consumerTag, envelope, properties, body) => {
      onMessage(consumerTag, envelope, properties, body)
    }
  }

  def onMessage(consumerTag:String, envelop:Envelope, props:BasicProperties, body:Array[Byte]):Unit

  protected def setupAmqpEndpoint(builder:AmqpBuilder):AmqpSettings

  override def receive:Receive = amqp.orElse(service.handle)
}

trait AmqpBuilder {
  def connect(uri:String):AmqpBuilder
  def declareExchange(name:String, exchangeType:String = ExchangeTypes.DIRECT, durable:Boolean = true, autodelete:Boolean = false, internal:Boolean = false, props:Map[String, AnyRef] = Map()):AmqpBuilder
  def declareQueue(name:String, durable:Boolean = true, exclusive:Boolean = false, autodelete:Boolean = false, props:Map[String, AnyRef] = Map()):AmqpBuilder
  def bind(exchange:String, queue:String, routingKey:String, props:Map[String, AnyRef] = Map()):AmqpBuilder
  def subscribe(queueName:String, autoack:Boolean = true):AmqpBuilder
  def build():AmqpSettings
}

private class AmqpBuilderImpl(val endpoint:ActorRef) extends AmqpBuilder {
  private var uri:String = _
  private var exchanges:Seq[DeclareExchange] = Seq()
  private var queues:Seq[DeclareQueue] = Seq()
  private var binds:Seq[BindQueue] = Seq()
  private var subscribes:Seq[Subscribe] = Seq()

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

  override def subscribe(queueName:String, autoack:Boolean):AmqpBuilder = {
    subscribes = subscribes ++ Seq(new Subscribe(queueName, autoack))
    this
  }

  override def build(): AmqpSettings = {
    new AmqpSettings(uri, exchanges, queues, binds, subscribes)
  }
}

case class AmqpSettings(uri:String, exchanges:Seq[Command], queues:Seq[Command], binds:Seq[Command], subscribes:Seq[Command])

object ExchangeTypes {
  val TOPIC = "topic"
  val FANOUT = "fanout"
  val DIRECT = "direct"
  val HEADERS = "headers"
}

trait SetupAmqpEndpoint extends (AmqpBuilder=>AmqpSettings) {
  def apply() = this
  def apply(builder:AmqpBuilder, k:(AmqpSettings)=>ActorRef):Tuple2[ActorRef, AmqpSettings]
}

object SetupAmqpEndpoint {
  def apply(m:AmqpBuilder=>AmqpSettings):SetupAmqpEndpoint = new SetupAmqpEndpoint {
    override def apply(builder:AmqpBuilder):AmqpSettings = {
      m(builder)
    }

    override def apply(builder:AmqpBuilder, k:(AmqpSettings) => ActorRef):Tuple2[ActorRef, AmqpSettings] = {
      val settings = m(builder)
      Tuple2(k(settings), settings)
    }
  }
}