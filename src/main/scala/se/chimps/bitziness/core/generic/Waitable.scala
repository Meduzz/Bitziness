package se.chimps.bitziness.core.generic

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Await, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

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
  implicit def waiting(future:Future[Any])(implicit executor:ExecutionContext):Waitable = {
    new WaitableImpl(future)
  }

  /**
   * This allows for a manual actions.
   * @param future
   * @return
   */
  def apply(future:Future[Any])(implicit executor:ExecutionContext):Waitable = {
    new WaitableImpl(future)
  }
}

trait Waitable {
  /**
   * Stay away, will eat threads!
   * @param tag
   * @tparam T
   * @return
   */
  def get[T](implicit tag:ClassTag[T]):T
  def getOrRecover[T](implicit recover:PartialFunction[Throwable, T], tag:ClassTag[T]):T
}

class WaitableImpl(val any:Future[Any])(implicit val duration:Option[Duration], executor:ExecutionContext) extends Waitable {
  def getOrRecover[T](implicit recover:PartialFunction[Throwable, T], tag:ClassTag[T]):T = {
    any.mapTo[T].value match {
      case Some(Success(t)) => t
      case Some(Failure(e)) => recover(e)
      case None => recover(new FutureReturnedNoneException("Thread returned None."))
    }
  }

  /**
   * Stay away, will eat threads!
   * @param tag
   * @tparam T
   * @return
   */
  override def get[T](implicit tag: ClassTag[T]):T = Await.result(any.mapTo[T], duration.getOrElse(Duration(3l, TimeUnit.SECONDS)))
}

class FutureReturnedNoneException(val message:String) extends RuntimeException(message) {
}