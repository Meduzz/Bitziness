package example

import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.Envelope
import se.chimps.bitziness.core.service.AbstractService
import se.chimps.bitziness.core.service.plugins.amqp.{ExchangeTypes, AmqpSettings, AmqpBuilder, Amqp}

class TestService extends AbstractService with Amqp {
  override def handle: Receive = {
    case x:AnyRef => println(s"TestService got an unspecified message, ${x}")
  }

  override def onMessage(consumerTag:String, envelop:Envelope, props:BasicProperties, body:Array[Byte]):Unit = {

  }

  override protected def setupAmqpEndpoint(builder:AmqpBuilder):AmqpSettings = {
    builder.connect("amqp://localhost:5672").declareExchange("test.exchange", ExchangeTypes.DIRECT, false)
      .declareQueue("test.queue").bind("test.exchange", "test.queue", "").build()
  }
}

case class Message(body:String)
