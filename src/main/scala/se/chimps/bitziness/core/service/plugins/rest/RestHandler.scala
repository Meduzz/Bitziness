package se.chimps.bitziness.core.service.plugins.rest

import akka.actor.Actor


/**
 * The handler that the web-server sees.
 * Will basically just route messages to the router-actor.
 */
class RestHandler extends Actor {
  override def receive: Receive = {
    case _ => println("RestHandler got a message")
    // TODO add a 404 responder at the end of each chain.
  }
}
