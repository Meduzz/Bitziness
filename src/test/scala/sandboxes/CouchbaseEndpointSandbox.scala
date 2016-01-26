package sandboxes

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.{PipeToSupport, ask}
import akka.util.Timeout
import com.couchbase.client.java.document.{JsonDocument, RawJsonDocument}
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment
import se.chimps.bitziness.core.Service
import se.chimps.bitziness.core.generic.Init
import se.chimps.bitziness.core.generic.Serializers.JSONSerializer
import se.chimps.bitziness.core.generic.persistence.couchbase.endpoint.CouchbaseEndpoint

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object CouchbaseEndpointSandbox extends App {
  val system = ActorSystem()
  val service = system.actorOf(Props(classOf[CouchbaseEndpointSandbox]))
  service ! Init

  implicit val timeout = Timeout(3L, TimeUnit.SECONDS)

  val doc1 = service ? Set("test", Value("test"))
  Await.ready(doc1, Duration(3L, TimeUnit.SECONDS))
  val doc2 = service ? Get("test")
  Await.ready(doc2, Duration(3L, TimeUnit.SECONDS))
  val doc3 = service ? Delete("test")
  Await.ready(doc3, Duration(3L, TimeUnit.SECONDS))

  doc1.mapTo[RawJsonDocument].map(doc => println(s"Set: ${doc.content()}")).recover({
    case e:Throwable => e.printStackTrace()
  })
  doc2.mapTo[JsonDocument].map(doc => println(s"Get: ${doc.content()}")).recover({
    case e:Throwable => e.printStackTrace()
  })
  doc3.mapTo[JsonDocument].map(doc => println(s"Del: ${doc.content()}")).recover({
    case e:Throwable => e.printStackTrace()
  })

  val future = Future(Thread.sleep(1500L))
  Await.ready(future, Duration(3L, TimeUnit.SECONDS))

  system.shutdown()
}

class CouchbaseEndpointSandbox extends Service with PipeToSupport {

  implicit val timeout = Timeout(3L, TimeUnit.SECONDS)
  var endpoint:ActorRef = _

  override def handle: Receive = {
    case s:Set => (endpoint ? s).pipeTo(sender())
    case g:Get => (endpoint ? g).pipeTo(sender())
    case d:Delete => (endpoint ? d).pipeTo(sender())
  }

  override def initialize(): Unit = {
    endpoint = initEndpoint(classOf[CouchbaseSandbox], "CouchbaseSandbox")
  }
}

class CouchbaseSandbox(val service:ActorRef) extends CouchbaseEndpoint with JSONSerializer with PipeToSupport {

  implicit val timeout = Timeout(3L, TimeUnit.SECONDS)

  override val nodes = List("192.168.235.20")
  override val env = DefaultCouchbaseEnvironment.builder()
      .bootstrapCarrierDirectPort(11211)
      .build()

  val bucket = "test"

  override def receive: Receive = {
    case Set(key, ref) => withBucket(bucket) { buck =>
      val doc = RawJsonDocument.create(key, toJSON(ref))
      buck.insert(doc)
    }.pipeTo(sender())
    case Get(key) => withBucket(bucket) { buck =>
      buck.get(key)
    }.pipeTo(sender())
    case Delete(key) => withBucket(bucket) { buck =>
      buck.remove(key)
    }.pipeTo(sender())
  }
}

case class Set(key:String, value:AnyRef)
case class Get(key:String)
case class Delete(key:String)
case class Value(value:String)