package se.chimps.bitziness.core.generic

import akka.actor.ActorRef

/**
 * Allows us to set a name on the actor.
 */
trait Naming { parent:ActorRef =>
  def name():String
}
