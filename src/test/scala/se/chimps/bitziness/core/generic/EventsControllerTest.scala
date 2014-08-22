package se.chimps.bitziness.core.generic

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKitBase, TestProbe}
import org.scalatest.FunSuite

/**
 * Tests for the events module.
 */
class EventsControllerTest extends FunSuite with TestKitBase {
  implicit lazy val system = ActorSystem("test")
  val eventStream = EventStreamImpl()

  test("events are published") {
    val probe = TestProbe()

    TestActorRef(Props(classOf[ZeEndpoint], probe.ref, classOf[Ping]))

    val msg = new Ping()
    eventStream.publish(msg)

    probe.expectMsg(msg)
  }

  test("events that are not subscribed to are not delivered") {
    val probe = TestProbe()

    TestActorRef(Props(classOf[ZeEndpoint], probe.ref, classOf[Ping]))

    val msg = new Pong()
    eventStream.publish(msg)

    probe.expectNoMsg()
  }
}

case class Ping() extends Event
case class Pong() extends Event

class ZeEndpoint(val probe:ActorRef, val subscribe:Class[Event]) extends Actor with Events {

  override def receive:Receive = {
    case x:Event => probe.forward(x)
  }

  @scala.throws[Exception](classOf[Exception])
  override def preStart():Unit = {
    this.internalEventsBuilder.subscribe(subscribe)
  }
}