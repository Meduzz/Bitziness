package se.chimps.bitziness.core

import akka.actor.{ActorRef, Actor}
import se.chimps.bitziness.core.services.healthcheck.HealthChecks

trait Endpoint extends Actor {
  def service:ActorRef

  def healthCheck(name:String, hc:()=>Boolean):Unit = HealthChecks.register(name, hc)
}