package se.chimps.bitziness.core.generic

import akka.actor.ActorRef
import akka.event.{SubchannelClassification, EventBus}
import akka.util.Subclassification
import se.chimps.bitziness.core.generic

/**
 * Base trait for internal events.
 */
trait Event {

}

class EventStreamImpl extends EventBus with SubchannelClassification {
  override type Event = generic.Event
  override type Classifier = Class[_]
  override type Subscriber = ActorRef

  override protected def publish(event:Event, subscriber:Subscriber):Unit = subscriber ! event

  override protected def classify(event:Event):Classifier = event.getClass

  override protected implicit def subclassification:Subclassification[Classifier] = new SubclassificationImpl
}

private class SubclassificationImpl extends Subclassification[Class[_]] {

  override def isEqual(x:Class[_], y:Class[_]):Boolean = x == y

  override def isSubclass(x:Class[_], y:Class[_]):Boolean = y isAssignableFrom x
}

object EventStreamImpl {
  private lazy val stream = new EventStreamImpl()

  def apply():EventStreamImpl = stream
}