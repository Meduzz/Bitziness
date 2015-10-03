package se.chimps.bitziness.core.endpoints.io

import akka.actor.{Actor, ActorRef}
import akka.io.Tcp._
import akka.util.ByteString

import scala.concurrent.Future

object Common {

  private[io] case object Ack extends Event

  trait ConnectionBase extends Actor {
    def connection:ActorRef

    def onData(data:ByteString):Future[ByteString]
    def onClose():Unit
    def onCommandFailed(cmd:CommandFailed):Unit
    def errorMapping:PartialFunction[Throwable, ByteString] = {
      case e:_ => ByteString.fromString(e.getMessage)
    }
    def write(data:ByteString):Unit = {
      connection ! Write(data, Ack)
    }
    def writeFile(file:String,  offset:Long, length:Long):Unit = {
      connection ! WriteFile(file, offset, length, Ack)
    }

    def connectionHandler:Receive = {
      case Closed => onClose()
      case PeerClosed => onClose()
      case d:Received => {
        onData(d.data)
          .recover(errorMapping)
          .foreach(bs => connection ! Write(bs, Ack))
      }
      case cmd:CommandFailed => onCommandFailed(cmd)
    }

    def otherConnectionLogic:Receive
  }
}
