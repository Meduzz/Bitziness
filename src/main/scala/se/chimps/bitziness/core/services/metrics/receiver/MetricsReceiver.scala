package se.chimps.bitziness.core.project.modules.metrics.receiver

import akka.actor.Actor
import se.chimps.bitziness.core.generic.Events

/**
 * This will be hidden behind a Dispatcher an receive all the services metrics.
 * TODO implement
 */
class MetricsReceiver extends Actor with Events {
  override def receive: Receive = {
    case _ => println("Poff, there went a metric...")
  }
}
