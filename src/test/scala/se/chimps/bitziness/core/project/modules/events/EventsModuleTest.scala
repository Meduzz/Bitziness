package se.chimps.bitziness.core.project.modules.events

import akka.actor.{Props, ActorSystem}
import akka.testkit.{TestKitBase, TestProbe, TestActorRef}
import org.scalatest.FunSuite
import se.chimps.bitziness.core.generic.{Event, EventStreamImpl}
import se.chimps.bitziness.core.project.Project
import se.chimps.bitziness.core.project.modules.registry.ServiceStarted
import se.chimps.bitziness.core.service.Service

/**
 * Tests for the events module.
 */
class EventsModuleTest extends FunSuite with TestKitBase {
  implicit lazy val system = ActorSystem("test")
  val eventStream = new EventStreamImpl

  test("registerEvent publishes an event") {
    val probe = TestProbe()

    eventStream.subscribe(probe.ref, classOf[TestMsg])

    val msg = new TestMsg("spam")
    val obj = new TestProject()
    obj.triggerMessage(msg)

    // This shite is not working...
    probe.expectMsg(msg)
  }
}

private class TestProject extends Project with Events {
  val actorSystem:ActorSystem = ActorSystem("test")

  override def initialize(args:Array[String]):Unit = {}

  def triggerMessage(msg:TestMsg) {
    eventStream.publish(msg)
  }
}

case class TestMsg(spam:String) extends Event