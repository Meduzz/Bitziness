package se.chimps.bitziness.core.endpoints.amqp

import akka.actor._
import io.github.drexin.akka.amqp.AMQP
import io.github.drexin.akka.amqp.AMQP._
import se.chimps.bitziness.core.Endpoint
import akka.io.IO

/**
 * Base trait for consuming and producing AMQP messages.
 */
trait AmqpEndpoint extends Endpoint {

  protected def setupAmqpEndpoint(builder:AmqpBuilder):AmqpSettings

  protected val (settings:AmqpSettings, connection:ActorRef) = SetupAmqpEndpoint(setupAmqpEndpoint)(new AmqpBuilderImpl, self)(context.system)

}

class OuterAmqpEndpoint(val settings:AmqpSettings, val endpoint:ActorRef) extends Actor with Stash with ActorLogging {

  var connection: ActorRef = _

  override def receive: Receive = {
    case Connected(_, con: ActorRef) => {
      connection = con
      settings.exchanges.foreach(cmd => {
        log.debug("DeclareExchange: {}", cmd)
        con ! cmd
      })
      settings.queues.foreach(cmd => {
        log.debug("DeclareQueue: {}", cmd)
        connection ! cmd
      })
    }
    case ExchangeDeclared(exchange:String) => log.debug("ExchangeDeclared: {}", exchange)
    case QueueDeclared(queue: String) =>
      log.debug("QueueDeclared: {}", queue)
      settings.binds.filter(c => c.asInstanceOf[BindQueue].queue.equals(queue)).foreach(cmd => {
        log.debug("Bind: {}", cmd)
        connection ! cmd
      })
    case QueueBound(queue: String, _, _) =>
      log.debug("QueueBound: {}", queue)
      settings.subscribes.filter(c => c.asInstanceOf[Subscribe].queue.equals(queue)).foreach(cmd => {
        log.debug("Subscribe: {}", cmd)
        connection ! cmd
      })
      unstashAll()
    case p: Publish =>
      log.debug("Publish: {}", p)
      if (connection != null) {
        connection ! p
      } else {
        stash()
      }
    case d: Delivery => {
      log.debug("Delivery: {}", d)
      endpoint ! d
    }
  }
}

trait AmqpBuilder {
  def connect(uri:String):AmqpBuilder
  def declareExchange(name:String, exchangeType:String = ExchangeTypes.DIRECT, durable:Boolean = true, autodelete:Boolean = false, internal:Boolean = false, props:Map[String, AnyRef] = Map()):AmqpBuilder
  def declareQueue(name:String, durable:Boolean = true, exclusive:Boolean = false, autodelete:Boolean = false, props:Map[String, AnyRef] = Map()):AmqpBuilder
  def bind(exchange:String, queue:String, routingKey:String, props:Map[String, AnyRef] = Map()):AmqpBuilder
  def subscribe(queueName:String, autoack:Boolean = true):AmqpBuilder
  def build():AmqpSettings
}

private class AmqpBuilderImpl extends AmqpBuilder {
  private var uri:String = _
  private var exchanges:Seq[DeclareExchange] = Seq()
  private var queues:Seq[DeclareQueue] = Seq()
  private var binds:Seq[BindQueue] = Seq()
  private var subscribes:Seq[Subscribe] = Seq()

  override def connect(uri: String): AmqpBuilder = {
    this.uri = uri
    this
  }

  override def declareExchange(name:String, exchangeType:String = ExchangeTypes.DIRECT, durable:Boolean = true, autodelete:Boolean = false, internal:Boolean = false, props:Map[String, AnyRef] = Map()): AmqpBuilder = {
    exchanges = exchanges ++ Seq(new DeclareExchange(name, exchangeType, durable, autodelete, internal, props))
    this
  }

  override def declareQueue(name:String, durable:Boolean = true, exclusive:Boolean = false, autodelete:Boolean = false, props:Map[String, AnyRef] = Map()): AmqpBuilder = {
    queues = queues ++ Seq(new DeclareQueue(name, durable, exclusive, autodelete, props))
    this
  }

  override def bind(exchange:String, queue:String, routingKey:String, props:Map[String, AnyRef] = Map()): AmqpBuilder = {
    binds = binds ++ Seq(new BindQueue(queue, exchange, routingKey, props))
    this
  }

  override def subscribe(queueName:String, autoack:Boolean = true):AmqpBuilder = {
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

trait SetupAmqpEndpoint {
  def apply(builder:AmqpBuilder, service:ActorRef)(implicit system:ActorSystem):Tuple2[AmqpSettings, ActorRef]
}

object SetupAmqpEndpoint {

  def apply(m:AmqpBuilder=>AmqpSettings):SetupAmqpEndpoint = new SetupAmqpEndpoint {

    override def apply(builder: AmqpBuilder, service:ActorRef)(implicit system:ActorSystem):Tuple2[AmqpSettings, ActorRef] = {
      val amqpExtension = IO(AMQP)
      val settings = m(builder)
      val outerEndpoint = system.actorOf(Props(classOf[OuterAmqpEndpoint], settings, service), distinct(settings.uri))
      amqpExtension.tell(Connect(settings.uri), outerEndpoint)
      Tuple2(settings, outerEndpoint)
    }
  }

  private def distinct(uri:String):String = {
    val s = uri.drop(7)
    if (s.contains("@")) {
      return s.dropWhile(c => !c.equals("@".charAt(0))).drop(1)
    } else {
      return s
    }
  }
}