package se.chimps.bitziness.core.endpoints.io

import akka.actor.{Actor, ActorRef}
import akka.io.Tcp._
import akka.util.ByteString

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Common {

  private[io] case object Ack extends Event

  trait ConnectionBase extends Actor {
    def connection:ActorRef

    def onData(data:ByteString):Future[Either[ByteString, Unit]]
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
          .recover(errorMapping)
          .foreach {
            case Left(bs) => write(bs)
            case Right(u) => u
          }
      }
      case cmd:CommandFailed => onCommandFailed(cmd)
    }

    def otherConnectionLogic:Receive
  }
}
