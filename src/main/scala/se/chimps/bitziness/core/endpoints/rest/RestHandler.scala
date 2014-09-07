package se.chimps.bitziness.core.endpoints.rest

import _root_.spray.can.Http.Register
import _root_.spray.http.HttpRequest
import akka.actor.Actor
import akka.io.Tcp.Connected
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Engine

/**
 * The handler that the web-server sees.
 */
class RestHandler(val definitions:EndpointDefinition) extends Actor with Engine {

  override def receive: Receive = {
    case connected:Connected => sender() ! Register(self)
      // TODO routing borde ligga i en egen actor som vi stoppar in bakom en "executor".
    case req:HttpRequest => {
      val spray = sender()
      val response = request(req)
      println(s"Response: ${response}")
      spray ! response
    }
    case x => println(s"RestHandler got a message ${x}.")
  }

}
