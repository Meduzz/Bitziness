package example

import se.chimps.bitziness.core.service.Service

class TestService extends Service {
  override def handle: Receive = {
    case _ => println("TestService got a message")
  }
}
