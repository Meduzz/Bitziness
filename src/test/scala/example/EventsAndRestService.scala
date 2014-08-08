package example

import se.chimps.bitziness.core.generic.Event
import se.chimps.bitziness.core.service.AbstractService
import se.chimps.bitziness.core.service.plugins.events.Events
import se.chimps.bitziness.core.service.plugins.rest.{EndpointDefinition, RestEndpointBuilder, REST}

class EventsAndRestService extends AbstractService with Events with REST {

  override def onEvent:Receive = {
    case m:Event => println(s"Events and Rest service received event: ${m}")
  }

  override def handle:Receive = {
    case msg:AnyRef => println(s"Events and Rest service received unknown message. ${msg}")
  }

  @scala.throws[Exception](classOf[Exception])
  override def preStart():Unit = internalEventsBuilder.subscribe(classOf[SpamEvent])

  override def initialize():Unit = {
    publish(new SpamEvent(s"Im (${getClass.getSimpleName}) alive!"))
  }

  override def configureRestEndpoint(builder: RestEndpointBuilder): EndpointDefinition = {
    builder.build()
  }
}
