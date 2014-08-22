package se.chimps.bitziness.core

import akka.actor.{ActorRef, Actor}

trait Endpoint extends Actor {
}

abstract class AbstractEndpoint(val service:ActorRef) extends Endpoint {

}