package se.chimps.bitziness.core.endpoints.io

import java.net.InetSocketAddress

import akka.actor.ActorRef
import akka.io.Inet.SocketOption
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import se.chimps.bitziness.core.Endpoint
import se.chimps.bitziness.core.endpoints.io.Common.ConnectionBase

abstract class ClientIOEndpoint(val service:ActorRef) extends Endpoint {
  implicit val system = context.system
  
  override def receive: Receive = {
    case c:ConnectCommand => {
      IO(Tcp) ! Connect(c.remote, c.local, c.settings)
    }
    case c:Connected => sender() ! Register(onConnection(sender()))
    case cmd:CommandFailed => onCommandFailed(cmd)
  }

  def onConnection(connection:ActorRef):ActorRef
  def onCommandFailed(cmd:CommandFailed):Unit
}

import scala.collection.immutable.Traversable
case class ConnectCommand(remote:InetSocketAddress, local:Option[InetSocketAddress] = None, settings:Traversable[SocketOption] = Nil)

trait ClientIOConnection extends ConnectionBase {
  override def receive: Receive = connectionHandler orElse otherConnectionLogic
}
