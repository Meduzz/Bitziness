package se.chimps.bitziness.core.endpoints.amqp

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.testkit.{TestProbe, TestKitBase}
import io.github.drexin.akka.amqp.AMQP.{Publish, Delivery}
import org.scalatest.{Assertions, BeforeAndAfterAll, Tag, FunSuite}

import scala.concurrent.duration.FiniteDuration

/**
 * Test of the amqp endpoint.
 */
class AmqpTest extends FunSuite with TestKitBase with BeforeAndAfterAll {
  implicit lazy val system = ActorSystem()
  val probe = TestProbe()
  val endpoint = system.actorOf(Props(classOf[MyService], probe.ref, "test", "testQueue", ""))

  test("send message and get it back", AMQP) {
    val message = "spam"
    endpoint ! new TestPublish(message)

    val response = probe.expectMsgAllClassOf(classOf[TestResponse])
    Assertions.assert(response(0).text.equals(message))
  }

  override protected def afterAll(): Unit = {
    system.shutdown()
  }
}

object AMQP extends Tag("Depends on a running rabbitmq")

class MyService(val service:ActorRef, exchange:String, queue:String, routing:String) extends AmqpEndpoint with ActorLogging {

  override protected def setupAmqpEndpoint(builder: AmqpBuilder): AmqpSettings = {
    builder.connect("amqp://localhost:5672")
        .declareExchange(exchange)
        .declareQueue(queue)
        .bind(exchange, queue, routing)
        .subscribe(queue)
        .build()
  }

  override def receive:Receive = {
    case d:Delivery => {
      service ! new TestResponse(new String(d.body))
    }
    case msg:TestPublish => {
      this.connection ! Publish(exchange, routing, msg.text.getBytes())
    }
  }

  @throws[Exception](classOf[Exception])
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.error("restarting actor {}", reason)
    super.preRestart(reason, message)
  }
}

case class TestPublish(text:String)
case class TestResponse(text:String)