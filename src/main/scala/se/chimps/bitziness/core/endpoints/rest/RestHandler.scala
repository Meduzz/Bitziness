package se.chimps.bitziness.core.endpoints.rest

import _root_.spray.can.Http
import _root_.spray.can.Http.Register
import _root_.spray.http.HttpRequest
import akka.actor.Actor
import akka.io.IO
import akka.io.Tcp.Connected
import akka.pattern.PipeToSupport
import org.slf4j.LoggerFactory
import se.chimps.bitziness.core.Host
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Engine
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * The handler that the web-server sees.
 */
class RestHandler(val host:Host) extends Actor with Engine with PipeToSupport {
  implicit val system = context.system
  private val log = LoggerFactory.getLogger(getClass.getName)

  override def receive: Receive = {
    case connected:Connected => sender() ! Register(self)
    case req:HttpRequest => {
      val spray = sender()
      request(req).pipeTo(spray)
    }
    case route:Routes => addActions(route)
    case x => println(s"RestHandler got an unhandled message ${x}.")
  }

  IO(Http) ! Http.Bind(self, host.host, host.port)
}
