package se.chimps.bitziness.core.generic

import akka.actor.ActorSystem
import akka.testkit.{TestProbe, TestKitBase}
import org.scalatest.FunSuite
import se.chimps.bitziness.core.generic.LocalSession.LocalSessionFactory
import Waitable._

/**
 * Some session tests.
 */
class SessionTest extends FunSuite with TestKitBase {
  lazy implicit val system = ActorSystem()
  lazy implicit val duration = None
  lazy implicit val executor = system.dispatcher

  val probe = TestProbe()

  test("localSession does sessiony stuff") {
    val session = LocalSessionFactory.load("test-session-id")

    assert(!session.isSet("test-key").get[Boolean])
    assert(session.set("test-key", "test-value").get[Boolean])
    assert(session.isSet("test-key").get[Boolean])
    assert(session.get[String]("test-key").get[Option[String]].get.equals("test-value"))

    LocalSessionFactory.destroy("test-session-id")
    assert(!session.isSet("test-key").get[Boolean])
  }
}
