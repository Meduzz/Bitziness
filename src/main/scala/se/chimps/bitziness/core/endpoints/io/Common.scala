package se.chimps.bitziness.core.endpoints.io

import akka.actor.{Actor, ActorRef}
import akka.io.Tcp._
import akka.util.ByteString

object Common {

  private[io] case object Ack extends Event

  trait ConnectionBase extends Actor {
    def connection:ActorRef

    def onData(data:ByteString):Unit
    def onClose():Unit
    def onCommandFailed(cmd:CommandFailed):Unit
    def errorMapping:PartialFunction[Throwable, Either[ByteString, Unit]] = {
      case e:Throwable => Left(ByteString.fromString(e.getMessage))
    }
    def write(data:ByteString):Unit = {
      connection ! Write(data, Ack)
    }
    def writeFile(file:String,  offset:Long, length:Long):Unit = {
      connection ! WriteFile(file, offset, length, Ack)
    }

    def connectionHandler:Receive = {
      case c:ConnectionClosed => onClose()
      case Aborted => onClose()
      case d:Received => {
        onData(d.data)
      }
      case cmd:CommandFailed => onCommandFailed(cmd)
      case DisconnectCommand => connection ! Close
    }

    def otherConnectionLogic:Receive
  }
}
