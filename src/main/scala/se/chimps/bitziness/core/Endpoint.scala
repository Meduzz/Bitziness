package se.chimps.bitziness.core

import akka.actor.Actor

trait Endpoint extends Actor {
}

case class Host(host:String, port:Int) {
  override def toString: String = s"${host}:${port}"
}
