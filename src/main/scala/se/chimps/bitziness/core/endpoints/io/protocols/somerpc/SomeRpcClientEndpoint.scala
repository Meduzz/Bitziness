package se.chimps.bitziness.core.endpoints.io.protocols.somerpc

import java.util.UUID

import akka.actor.{ActorRef, Props}
import akka.io.Tcp._
import akka.util.ByteString
import se.chimps.bitziness.core.endpoints.io.protocols.somerpc.Common.{NewConnection, Error, RpcHelpers}
import se.chimps.bitziness.core.endpoints.io.{ClientIOConnection, ClientIOEndpoint}

import scala.concurrent.Future

class SomeRpcClientEndpoint(override val service:ActorRef, requestHandler:((Int, UUID, Seq[ByteString]))=>Future[(UUID, Seq[ByteString])]) extends ClientIOEndpoint(service) {
  override def onCommandFailed(cmd: CommandFailed): Unit = {
    val msg = cmd.cmd match {
      case b:Connect => s"Could not connect to ${b.remoteAddress.getHostString}."
      case r:Register => s"Could not register client with connection."
    }
    service ! Error(self, msg)
  }

  override def onConnection(connection: ActorRef): ActorRef = {
    val c = context.system.actorOf(Props(classOf[SomeRpcClientConnection], connection, service, requestHandler))
    service ! NewConnection(c)
    c
  }
}

class SomeRpcClientConnection(val connection:ActorRef, val service:ActorRef, override val handleRequest:((Int, UUID, Seq[ByteString]))=>Future[(UUID, Seq[ByteString])]) extends ClientIOConnection with RpcHelpers