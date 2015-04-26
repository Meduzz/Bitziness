package se.chimps.bitziness.core.endpoints.rest

import _root_.spray.can.Http
import _root_.spray.can.Http.Register
import _root_.spray.http.HttpRequest
import akka.actor.{ActorRef, Actor}
import akka.io.IO
import akka.io.Tcp.{Bind, CommandFailed, Bound, Connected}
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
    case req:HttpRequest => handle(req, sender())
    case route:Routes => addActions(route)
    case bound:Bound => // do naathing.
    case CommandFailed(_:Bind) => throw new RuntimeException(s"Could not bind to ${host.toString}.")
    case x => log.debug("RestHandler got an unhandled message {}.", x)
  }

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.debug(s"Connection actor for ${host.toString} started.")
    IO(Http) ! Http.Bind(self, host.host, host.port)
  }
}
