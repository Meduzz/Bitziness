package se.chimps.bitziness.core.endpoints.io

import java.net.InetSocketAddress

import akka.actor._
import akka.io.Inet.SocketOption
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import se.chimps.bitziness.core.Endpoint
import se.chimps.bitziness.core.endpoints.io.Common.ConnectionBase

abstract class ServerIOEndpoint(val service:ActorRef) extends Endpoint with ActorLogging {

  var connectionHandlers:Map[ActorRef, ActorRef] = Map()

  override def supervisorStrategy: SupervisorStrategy = SupervisorStrategy.stoppingStrategy

  override def receive: Receive = {
    case b:BindCommand => {
      IO(Tcp) ! Bind(self, b.local, b.backlog, b.settings)
    }
    case u:UnbindCommand => IO(Tcp) ! Unbind
    case b:Bound => log.info("Bind successful.")
    case Connected(remote, local) => {
      onConnection(remote, sender()) match {
        case Some(con:ActorRef) => {
          connectionHandlers ++ Map(sender() -> con)
          sender() ! Register(con)
        }
        case None => sender() ! Close
      }
    }
    case cmd:CommandFailed => onCommandFailed(cmd)
  }

  def onConnection(remote:InetSocketAddress, connection:ActorRef):Option[ActorRef]
  def onCommandFailed(cmd:CommandFailed):Unit
}

import scala.collection.immutable.Traversable
case class BindCommand(local:InetSocketAddress, backlog:Int = 100, settings:Traversable[SocketOption] = Nil)
case class UnbindCommand()
case class CloseAllCommand()

trait ServerIOConnection extends ConnectionBase {
  override def receive: Receive = connectionHandler orElse otherConnectionLogic
}