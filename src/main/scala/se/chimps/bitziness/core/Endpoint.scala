package se.chimps.bitziness.core

import akka.actor.{ActorRef, Actor}
import se.chimps.bitziness.core.generic.ErrorMapping
import se.chimps.bitziness.core.services.healthcheck.HealthChecks

trait Endpoint extends Actor with ErrorMapping {
  def service:ActorRef

  def healthCheck(name:String, hc:()=>Boolean):Unit = HealthChecks.register(name, hc)
}

case class Host(host:String, port:Int) {
  override def toString: String = s"${host}:${port}"
}
