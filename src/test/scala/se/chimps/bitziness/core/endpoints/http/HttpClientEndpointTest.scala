package se.chimps.bitziness.core.endpoints.http

import java.util.concurrent.TimeUnit

import akka.actor.{Props, ActorSystem, ActorRef}
import akka.http.scaladsl.model.headers.Connection
import akka.http.scaladsl.model.HttpResponse
import akka.pattern.PipeToSupport
import akka.stream.actor.ActorPublisherMessage.Cancel
import akka.testkit.{TestProbe, TestKitBase}
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import se.chimps.bitziness.core.endpoints.http.client.RequestBuilders

import scala.collection.immutable._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

/**
 * TESTS!!1111ONEONE
 */
class HttpClientEndpointTest extends FunSuite with TestKitBase with BeforeAndAfterAll {

  lazy implicit val system = ActorSystem("akka-http-client")

  test("huston, we have touchdown?") {
    val probe = TestProbe()
    val endpoint = system.actorOf(Props(classOf[HttpEndpoint], probe.ref, "duckduckgo.com", 443, true))

    endpoint.tell("/?q=Bitziness", probe.ref)

    probe.expectMsgPF(Duration(3L, TimeUnit.SECONDS)) {
      case response:HttpResponse => {
        assert(response.status.intValue() == 200)
        endpoint.tell("close", probe.ref)
      }
    }
  }

  override protected def afterAll():Unit = {
    system.shutdown()
  }
}

class HttpEndpoint(val service:ActorRef, val host:String, val port:Int, val secure:Boolean) extends HttpClientEndpoint with RequestBuilders with PipeToSupport {

  def receive:Receive = {
    case "close" => connection ! Cancel
    case text:String => {
      val s = sender()
      send(request("GET", text).withHeaders(Seq(Connection(Seq("close"))))).pipeTo(s)
    }
  }

  override def setupConnection(builder:ConnectionBuilder):ActorRef = {
    builder.host(host, port)
    builder.build(secure)
  }
}