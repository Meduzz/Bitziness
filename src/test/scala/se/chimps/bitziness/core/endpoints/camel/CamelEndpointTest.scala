package se.chimps.bitziness.core.endpoints.camel

import java.util.concurrent.TimeUnit

import akka.actor.{Props, ActorSystem}
import akka.camel.{CamelExtension, CamelMessage}
import akka.testkit.{TestProbe, TestKitBase}
import akka.util.Timeout
import org.scalatest.{Assertions, BeforeAndAfterAll, FunSuite}

import scala.util.{Success, Failure}

/**
 * Tests for Camel endpoints.
 */
class CamelEndpointTest extends FunSuite with TestKitBase with BeforeAndAfterAll {
  implicit lazy val system = ActorSystem()
  implicit lazy val camel = CamelExtension(system)
  implicit lazy val camelContext = camel.context
  implicit val timeout = Timeout(1L, TimeUnit.SECONDS)
  implicit val executionContext = system.dispatcher

  val probe = TestProbe()

  val consumer = system.actorOf(Props(classOf[MyConsumer]))
  val producer = system.actorOf(Props(classOf[MyProducer]))

  test("the happy case is happy, camel version") {

    val future = camel.activationFutureFor(consumer)

    future.onComplete {
      case Success(_) => producer.tell("spam", probe.ref)
      case Failure(e:Exception) => Assertions.fail(e)
      case _ => Assertions.fail("Unexpected Try type...")
    }

    val response = probe.expectMsgClass(classOf[CamelMessage])
    Assertions.assert(response.bodyAs[String].equals("maps"))
  }

  override protected def afterAll(): Unit = {
    system.shutdown()
    super.afterAll()
  }
}

class MyConsumer extends CamelConsumerEndpoint {
  override def endpointUri: String = "direct:test"

  override def receive: Receive = {
    case msg:CamelMessage => sender() ! msg.bodyAs[String].reverse
  }
}

class MyProducer extends CamelProducerEndpoint {
  override def endpointUri: String = "direct:test"
}