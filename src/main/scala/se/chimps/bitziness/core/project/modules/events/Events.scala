package se.chimps.bitziness.core.project.modules.events

import akka.actor.ActorSystem
import se.chimps.bitziness.core.generic.Event
import se.chimps.bitziness.core.project.Project
import se.chimps.bitziness.core.project.modules.Module

/**
 * A limited version of the events plugin for services. It can only publish events.
 */
trait Events extends Module { project:Project =>
  def actorSystem:ActorSystem

  def publish[T<:Event](event:T) = {
    actorSystem.eventStream.publish(event)
  }

}
