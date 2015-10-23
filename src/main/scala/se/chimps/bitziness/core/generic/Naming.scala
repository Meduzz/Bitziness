package se.chimps.bitziness.core.generic

import akka.actor.Actor

/**
 * Allows us to set a name on the actor.
 */
trait Naming { parent:Actor =>
  def name():String
}
