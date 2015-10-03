package se.chimps.bitziness.core.endpoints.somerpc.common

import java.util.UUID

import akka.actor.ActorRef

import scala.concurrent.Promise

/**
 *
 */
object Model {
  case class Transmit(method:Int, promise:Promise[Seq[Array[Byte]]], data:Seq[Array[Byte]])
  class Connection(val actor:ActorRef)

  sealed trait Event

  case class Handle(method:Int, uuid:UUID, data:Seq[Array[Byte]], connection: Connection) extends Event
}
