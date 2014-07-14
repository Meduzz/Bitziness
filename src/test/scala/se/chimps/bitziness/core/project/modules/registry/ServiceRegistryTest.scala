package se.chimps.bitziness.core.project.modules.registry

import akka.actor.ActorSystem
import org.scalatest.FunSuite
import se.chimps.bitziness.core.project.Project
import se.chimps.bitziness.core.service.Service

/**
 * Tests for ServiceRegistry.
 */
class ServiceRegistryTest extends FunSuite {

  test("services are added to list") {
    val obj = new TestSubject()
    obj.registerService(classOf[TestService])

    assert(obj.getServices().length == 1)
  }

  test("getServices are immutable") {
    val obj = new TestSubject()
    obj.registerService(classOf[TestService])
    obj.getServices().drop(1)

    assert(obj.getServices().length == 1)
  }

}

private class TestSubject extends Project {
  override def initialize(args:Array[String]):Unit = {}

  override def actorSystem:ActorSystem = ActorSystem()
}

private class TestService extends Service {
  override def handle:Receive = {
    case x:Any => unhandled(x)
  }
}