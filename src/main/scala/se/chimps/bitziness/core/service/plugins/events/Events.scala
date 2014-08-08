package se.chimps.bitziness.core.service.plugins.events

import se.chimps.bitziness.core.generic.{EventStreamImpl, Event}
import se.chimps.bitziness.core.service.Service
import se.chimps.bitziness.core.service.plugins.Plugin

/**
 * A feature that allows services to generate and subscribe for internal events.
 */
trait Events extends Plugin { service:Service =>
  val eventStream = EventStreamImpl()

  registerReceive(onEvent)

  def publish[T<:Event](event:T) = {
    eventStream.publish(event)
  }

  /**
   * A place for inter service events to be handled, or leave it empty.
   * @return
   */
  def onEvent:Receive

  val internalEventsBuilder:Builder = new Builder {
    override def subscribe[T <: Event](event:Class[T]):Unit = {
      eventStream.subscribe(self, event)
    }

    override def unsubscribe[T <: Event](event:Class[T]):Unit = {
      eventStream.unsubscribe(self, event)
    }
  }
}

trait Builder {
  def subscribe[T<:Event](event:Class[T])
  def unsubscribe[T<:Event](event:Class[T])
}