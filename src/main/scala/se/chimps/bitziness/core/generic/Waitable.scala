package se.chimps.bitziness.core.generic

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag

/**
 * An attempt to get rid of some boilerplatish future handling
 * TODO this could potentially be renamded to Implicits and collect other helpers & implicit explicits.
 */
trait Waitable {
  implicit def duration:Option[Duration] = None
  implicit def waiting(future:Future[Any]):Awaiting = {
    new Awaiting(future)
  }
}

class Awaiting(val any:Future[Any])(implicit val duration:Option[Duration]) {
  def get[T](implicit tag:ClassTag[T]):T = {
    Await.result(any.mapTo[T], duration.getOrElse(Duration(3l, TimeUnit.SECONDS)))
  }
}
