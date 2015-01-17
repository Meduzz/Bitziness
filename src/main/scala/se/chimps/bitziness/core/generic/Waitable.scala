package se.chimps.bitziness.core.generic

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag

/**
 * An attempt to get rid of some boilerplatish future handling
 */
object Waitable {
  implicit def duration:Option[Duration] = None

  /**
   * This allows for implicit explicits.
   * @param future
   * @return
   */
  implicit def waiting(future:Future[Any]):Waitable = {
    new WaitableImpl(future)
  }

  /**
   * This allows for a manual actions.
   * @param future
   * @return
   */
  def apply(future:Future[Any]):Waitable = {
    new WaitableImpl(future)
  }
}

trait Waitable {
  def get[T](implicit tag:ClassTag[T]):T
}

class WaitableImpl(val any:Future[Any])(implicit val duration:Option[Duration]) extends Waitable {
  def get[T](implicit tag:ClassTag[T]):T = {
    Await.result(any.mapTo[T], duration.getOrElse(Duration(3l, TimeUnit.SECONDS)))
  }
}
