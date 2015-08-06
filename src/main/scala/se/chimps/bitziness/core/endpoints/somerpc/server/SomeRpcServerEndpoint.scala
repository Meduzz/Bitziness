package se.chimps.bitziness.core.endpoints.somerpc.server

import akka.actor.ActorRef
import akka.io.Tcp.Connected
import se.chimps.bitziness.core.Endpoint

/**
 *
 */
class SomeRpcServerEndpoint(val service:ActorRef) extends Endpoint {

  override def receive: Receive = {
    case Connected(remote, local) => {
      // TODO spawn new connection actor.
      // TODO register the new actor with the new connection.
    }
  }

}
