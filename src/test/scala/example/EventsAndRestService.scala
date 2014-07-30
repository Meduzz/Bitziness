package example

import se.chimps.bitziness.core.generic.Event
import se.chimps.bitziness.core.service.AbstractService
import se.chimps.bitziness.core.service.plugins.events.Events

class EventsAndRestService extends AbstractService with Events {

  override def onEvent:Receive = {
    case m:Event => println(s"Events and Rest service received event: ${m}")
  }

  override def handle:Receive = {
    case msg:AnyRef => println(s"Events and Rest service received unknown message. ${msg}")
  }

  @scala.throws[Exception](classOf[Exception])
  override def preStart():Unit = builder.subscribe(classOf[SpamEvent])

  override def initialize():Unit = {
    publish(new SpamEvent(s"Im (${getClass.getSimpleName}) alive!"))
  }
}
