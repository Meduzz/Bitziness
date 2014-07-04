package se.chimps.bitziness.core.service

import akka.actor.Actor

/**
 * The base class for all services.
 * This is where most of your buisiness logic will reside or be knit together.
 */
abstract class Service extends Actor {
  def handle:Receive

  override def receive:Actor.Receive = handle

  // TODO intorduce service events, or application events. A method like Receive but for events generated internally.
}
