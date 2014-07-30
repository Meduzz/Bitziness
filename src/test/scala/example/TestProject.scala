package example

import se.chimps.bitziness.core.project.AbstractProject
import se.chimps.bitziness.core.project.modules.events.Events

object TestProject extends AbstractProject with Events {

  override def initialize(args:Array[String]):Unit = {

    registerService(classOf[AmqpAndEventsService])
    registerService(classOf[EventsAndRestService])

  }
}
