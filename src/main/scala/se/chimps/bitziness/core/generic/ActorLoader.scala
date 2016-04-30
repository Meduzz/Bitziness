package se.chimps.bitziness.core.generic

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.util.Timeout

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Promise, Future}
import scala.reflect.ClassTag
import scala.util.{Try, Failure, Success, Random}

trait ActorLoader {
  def loadOrCreate[T <: Actor](path:String, factory:ActorFactory[T])(implicit ctx:ActorRefFactory, timeout:Timeout, evidence:ClassTag[T]):Future[ActorRef] = {
    implicit val ec = ctx.dispatcher
		val promise = Promise[ActorRef]()

		Try(ctx.actorOf(Props(factory.actor), factory.name)) match {
			case s:Success[ActorRef] => promise.complete(s)
			case f:Failure[ActorRef] => createActor(promise, path)
		}
		promise.future
  }

	private def createActor(promise:Promise[ActorRef], path:String, lock:Int = 0)(implicit ctx:ActorRefFactory, ec:ExecutionContext):Unit = {
		Thread.sleep(Random.nextInt(1000))
		val actor = ctx.actorSelection(path).resolveOne(Duration(3, TimeUnit.SECONDS))
		actor.onComplete {
			case s:Success[ActorRef] => {
				promise.complete(s)
			}
			case f:Failure[ActorRef] => {
				if (lock > 3) {
					promise.complete(f)
				} else {
					createActor(promise, path, lock + 1)
				}
			}
		}
	}
}
