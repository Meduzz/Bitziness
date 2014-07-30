package example

import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.Envelope
import se.chimps.bitziness.core.generic.Event
import se.chimps.bitziness.core.service.AbstractService
import se.chimps.bitziness.core.service.plugins.amqp.{ExchangeTypes, AmqpSettings, AmqpBuilder, Amqp}
import se.chimps.bitziness.core.service.plugins.events.Events

class AmqpAndEventsService extends AbstractService with Amqp with Events {

  override def handle: Receive = {
    case x:AnyRef => println(s"Amqp Service got an unspecified message, ${x}")
  }

  override def onMessage(consumerTag:String, envelop:Envelope, props:BasicProperties, body:Array[Byte]):Unit = {
    println(s"Amqp servic got a funny message for routing key: ${envelop.getRoutingKey}.")
  }

  override protected def setupAmqpEndpoint(amqpBuilder:AmqpBuilder):AmqpSettings = {
    amqpBuilder.connect("amqp://localhost:5672").declareExchange("test.exchange", ExchangeTypes.DIRECT, false)
      .declareQueue("test.queue").bind("test.exchange", "test.queue", "").build()
  }

  /**
   * A place for events to be handled, or leave it empty.
   * @return
   */
  override def onEvent:Receive = {
    case SpamEvent(body) => println(s"amqp received: ${body}.")
  }

  @scala.throws[Exception](classOf[Exception])
  override def preStart():Unit = {
    builder.subscribe(classOf[SpamEvent])
  }

  override def initialize():Unit = {
    publish(new SpamEvent(s"Im (${getClass.getSimpleName}) alive!"))
  }
}

case class SpamEvent(body:String) extends Event
