package se.chimps.bitziness.core.project.modules.events

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.testkit.{TestKitBase, TestProbe, TestActorRef}
import org.scalatest.FunSuite
import se.chimps.bitziness.core.generic.{Event, EventStreamImpl}
import se.chimps.bitziness.core.service.Service
import se.chimps.bitziness.core.service.plugins.events

/**
 * Tests for the events module.
 */
class EventsModuleTest extends FunSuite with TestKitBase {
  implicit lazy val system = ActorSystem("test")
  val eventStream = EventStreamImpl()

  test("events are published") {
    val probe = TestProbe()

    TestActorRef(Props(classOf[ZeService], probe.ref, classOf[Ping]))

    val msg = new Ping()
    eventStream.publish(msg)

    probe.expectMsg(msg)
  }

  test("events that are not subscribed to are not delivered") {
    val probe = TestProbe()

    TestActorRef(Props(classOf[ZeService], probe.ref, classOf[Ping]))

    val msg = new Pong()
    eventStream.publish(msg)

    probe.expectNoMsg()
  }
}

case class Ping() extends Event
case class Pong() extends Event

class ZeService(val probe:ActorRef, val subscribe:Class[Event]) extends Service with events.Events {

  override def handle:Receive = {
    case x:Event => probe.forward(x)
  }

  /**
   * A place for events to be handled, or leave it empty.
   * @return
   */
  override def onEvent:Receive = {
    case ping:Ping => {
      probe.forward(ping)
    }
    case pong:Pong => {
      probe.forward(pong)
    }
  }

  @scala.throws[Exception](classOf[Exception])
  override def preStart():Unit = {
    this.internalEventsBuilder.subscribe(subscribe)
  }
}