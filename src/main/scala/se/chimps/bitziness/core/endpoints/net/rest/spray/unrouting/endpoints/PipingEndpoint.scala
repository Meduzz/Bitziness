package se.chimps.bitziness.core.endpoints.net.rest.spray.unrouting.endpoints

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.util.Timeout
import se.chimps.bitziness.core.endpoints.net.rest.RESTEndpoint
import akka.pattern.pipe
import scala.concurrent.ExecutionContext.Implicits._
import akka.pattern.ask

/**
 * Will pipe all messages to the service and back.
 */
abstract class PipingEndpoint(val service:ActorRef) extends RESTEndpoint {
  implicit val timeout = Timeout(1, TimeUnit.SECONDS)

  override def rest: Receive = {
    case msg:Any => (this.service ? msg).pipeTo(sender())
  }
}
