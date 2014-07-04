package example

import se.chimps.bitziness.core.service.Service
import se.chimps.bitziness.core.service.plugins.amqp.{AmqpSettings, AmqpBuilder, Amqp}

class TestService extends Service with Amqp {
  override def handle: Receive = {
    case _ => println("TestService got a message")
  }

  override protected def setupAmqpEndpoint(builder:AmqpBuilder):AmqpSettings = {
    builder.connect("amqp://guest:guest@localhost:5672").build()
  }
}
