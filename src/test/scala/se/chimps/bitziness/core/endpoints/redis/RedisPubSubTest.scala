package se.chimps.bitziness.core.endpoints.redis

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestKitBase, TestProbe}
import org.scalatest._
import redis.RedisPubSub
import redis.api.pubsub.{Message, PMessage}

/**
 * Test the redis pubsub system.
 */
class RedisPubSubTest extends FunSuite with TestKitBase with BeforeAndAfterAll {
  implicit lazy val system: ActorSystem = ActorSystem("RedisPubSub")

  val probe = TestProbe()
  val endpoint = system.actorOf(Props(classOf[MyService], probe.ref))

  test("the happy case is happy", Redis) {

    Thread.sleep(200L)

    endpoint ! new RedisPublish("a.b.c", "test")

    val abc = probe.expectMsgClass(classOf[Message])
    val abc2 = probe.expectMsgClass(classOf[PMessage])
    Assertions.assert(abc.data.equals("test"))
    Assertions.assert(abc2.data.equals("test"))

    endpoint ! new RedisPublish("a.b.d", "spam")

    val abd = probe.expectMsgClass(classOf[PMessage])
    Assertions.assert(abd.data.equals("spam"))
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    system.shutdown()
  }
}

object Redis extends Tag("Depends on a running redis")

class MyService(val service:ActorRef) extends RedisPubSubEndpoint with RedisPubSubMethods {

  override protected def setupRedisPubSub(builder: RedisBuilder):RedisPubSub = {
    builder.subscribeChannel("a.b.c", "test")
      .subscribePattern("a.b.?", "te*")
      .build()
  }

  override def receive:Receive = {
    case RedisPublish(channel, text) => publish(channel, text)
    case msg:Message => service ! msg
    case msg:PMessage => service ! msg
  }
}

case class RedisPublish(channel:String, text:String)