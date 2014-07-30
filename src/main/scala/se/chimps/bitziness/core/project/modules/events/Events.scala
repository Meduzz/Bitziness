package se.chimps.bitziness.core.project.modules.events

import se.chimps.bitziness.core.generic.{EventStreamImpl, Event}
import se.chimps.bitziness.core.project.Project
import se.chimps.bitziness.core.project.modules.Module

/**
 * A limited version of the events plugin for services. It can only publish events.
 */
trait Events extends Module { project:Project =>
  val eventStream = EventStreamImpl()

  def publish[T<:Event](event:T) = {
    eventStream.publish(event)
  }

}
