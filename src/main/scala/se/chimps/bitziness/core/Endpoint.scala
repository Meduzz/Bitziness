package se.chimps.bitziness.core

import akka.actor.{ActorRef, Actor}

trait Endpoint extends Actor {
  def service:ActorRef
}

case class Host(host:String, port:Int) {
  override def toString: String = s"${host}:${port}"
}
