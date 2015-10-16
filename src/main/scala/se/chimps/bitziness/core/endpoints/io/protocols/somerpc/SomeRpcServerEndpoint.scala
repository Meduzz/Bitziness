package se.chimps.bitziness.core.endpoints.io.protocols.somerpc

import java.net.InetSocketAddress
import java.util.UUID

import akka.actor.{ActorRef, Props}
import akka.io.Tcp.{Bind, CommandFailed, Register}
import akka.util.ByteString
import se.chimps.bitziness.core.endpoints.io.{ServerIOConnection, ServerIOEndpoint}
import se.chimps.bitziness.core.endpoints.io.protocols.somerpc.Common.{NewConnection, RpcHelpers, Error}

import scala.concurrent.Future

class SomeRpcServerEndpoint(override val service:ActorRef, val requestHandler:((Int, UUID, Seq[ByteString]))=>Future[(UUID, Seq[ByteString])]) extends ServerIOEndpoint(service) {
  override def onCommandFailed(cmd: CommandFailed): Unit = {
    val msg = cmd.cmd match {
      case b:Bind => s"Could not bind to ${b.localAddress.getHostString}."
      case r:Register => s"Could not register server with connection."
    }
    service ! Error(self, msg)
  }

  override def onConnection(remote:InetSocketAddress, connection:ActorRef): Option[ActorRef] = {
    val c = Some(context.system.actorOf(Props(classOf[SomeRpcClientConnection], connection, service, requestHandler)))
    service ! NewConnection(c.get)
    c
  }
}

class SomeRpcServerConnection(val connection:ActorRef, val service:ActorRef, override val handleRequest:((Int, UUID, Seq[ByteString]))=>Future[(UUID, Seq[ByteString])]) extends ServerIOConnection with RpcHelpers