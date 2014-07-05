package se.chimps.bitziness.core.service

import akka.actor.Actor
import se.chimps.bitziness.core.generic.HasFeature

/**
 * The base class for all services.
 * This is where most of your buisiness logic will reside or be knit together.
 */
abstract class Service extends Actor with HasFeature {
  def handle:Receive

  override def receive:Actor.Receive = handle

  // TODO intorduce service events, or application events. A method like Receive but for events generated internally.
}
