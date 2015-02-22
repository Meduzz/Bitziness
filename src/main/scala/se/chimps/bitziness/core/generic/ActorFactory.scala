package se.chimps.bitziness.core.generic

import akka.actor.{Actor, ActorRef}

/**
 *
 */
trait ActorFactory[T<:Actor] {
  def actor():ActorRef
  def name():String
}
