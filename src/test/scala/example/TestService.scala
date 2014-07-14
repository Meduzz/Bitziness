package example

import se.chimps.bitziness.core.service.AbstractService
import se.chimps.bitziness.core.service.plugins.amqp.{AmqpSettings, AmqpBuilder, Amqp}

class TestService extends AbstractService with Amqp {
  override def handle: Receive = {
    case Message(body) => println(s"Received msg: ${body}")
    case _ => println("TestService got an unspecified message")
  }

  override protected def setupAmqpEndpoint(builder:AmqpBuilder):AmqpSettings = {
    val settings = builder.connect("amqp://guest:guest@localhost:5672").declareExchange("test.exchage").
      declareQueue("test.queue").bind("test.exchange", "test.queue", "").build()

    subscribe("test.queue", true, { deliver =>
      val service = self
      service ! new Message(new String(deliver.body, "utf-8"))
    })

    settings
  }
}

case class Message(body:String)
