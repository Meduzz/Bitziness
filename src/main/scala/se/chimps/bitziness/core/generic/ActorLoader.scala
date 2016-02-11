package se.chimps.bitziness.core.generic

import akka.actor._
import akka.util.Timeout

import scala.concurrent.Future
import scala.reflect.ClassTag

/**
  * Can be used in very simple situations where you require a certain actor (that happens to have an empty constructor).
  */
trait ActorLoader {
  def loadOrCreate[T](path:String, name:String)(implicit ctx:ActorRefFactory, timeout:Timeout, evidence:ClassTag[T]):Future[ActorRef] = {
    implicit val ec = ctx.dispatcher

    ctx.actorSelection(path).resolveOne().recover {
      case e:Throwable => ctx.actorOf(Props(evidence.runtimeClass), name)
    }
  }
}
