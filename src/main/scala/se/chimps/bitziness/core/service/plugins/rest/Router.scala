package se.chimps.bitziness.core.service.plugins.rest

import akka.actor.Actor
import se.chimps.bitziness.core.service.plugins.rest.exception.RouteNotFoundException

import scala.util.{Failure, Try}

/**
 * This bit of code will route web-traffic to its correct place in the service architecture.
 * The thought is that each route will be very thin and at best do some validation & conversion and then
 * ask the service for a response to send back.
 */
class Router(val routes:Map[String, (Request)=>Try[Response]]) extends Actor {

  override def receive: Receive = {
    case r:Request => sender ! matchAndRun(r)
  }

  private def matchAndRun(req:Request):Try[Response] = {
    val handler = routes(req.path)

    if (handler == null) {
      return Failure(new RouteNotFoundException(s"Could not find any handlers matching route: ${req.path}"))
    }

    handler(req)
  }

}
