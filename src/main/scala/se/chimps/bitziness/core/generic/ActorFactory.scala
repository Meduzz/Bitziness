package se.chimps.bitziness.core.generic

import akka.actor.{Actor}

/**
 *
 */
trait ActorFactory[T<:Actor] {
  def actor:T
  def name:String
}

object ActorFactory {
  def apply[T <: Actor](actorName:String, op:()=>T):ActorFactory[T] = new ActorFactory[T] {
    override def actor:T = op()

    override def name:String = actorName
  }
}
