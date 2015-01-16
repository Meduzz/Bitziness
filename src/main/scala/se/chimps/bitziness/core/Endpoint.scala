package se.chimps.bitziness.core

import akka.actor.{ActorRef, Actor}
import se.chimps.bitziness.core.generic.ErrorMapping

trait Endpoint extends Actor with ErrorMapping {
  def service:ActorRef
}

case class Host(host:String, port:Int) {
  override def toString: String = s"${host}:${port}"
}
