package se.chimps.bitziness.core.generic

import akka.actor.ActorRef

/**
 *
 */
trait ActorFactory[T<:ActorRef] {
  def actor():ActorRef
  def name():String
}
