package se.chimps.bitziness.core.service.plugins.rest

import akka.actor.{ActorRef, Actor}


/**
 * The handler that the web-server sees.
 * Will basically just route messages to the router-actor.
 */
class RestHandler(val router:ActorRef) extends Actor {
  override def receive: Receive = {
    case _ => println("RestHandler got a message")
  }
}
