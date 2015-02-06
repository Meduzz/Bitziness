package example

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.util.Timeout
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Action
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Framework.Controller
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Model.Responses.Ok
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.view.Scalate
import se.chimps.bitziness.core.endpoints.rest.{EndpointDefinition, RestEndpointBuilder, RESTEndpoint}
import se.chimps.bitziness.core.generic.Waitable._
import se.chimps.bitziness.core.{Service}
import akka.pattern._
import scala.concurrent.ExecutionContext.global

class PingService extends Service {
  override def handle:Receive = {
    case "PING" => sender() ! "PONG"
  }

  override def initialize():Unit = {
    initEndpoint[PingEndpoint](classOf[PingEndpoint], "Http")
  }
}

class PingEndpoint(val service:ActorRef) extends RESTEndpoint {
  implicit val executor = global

  override def configureRestEndpoint(builder:RestEndpointBuilder):EndpointDefinition = {
    val controller = new PingController(self)
    builder.mountController("", controller)
    builder.build()
  }

  override def receive:Receive = {
    case s:String =>
      implicit val timeout = Timeout(3l, TimeUnit.SECONDS)
      val sender:ActorRef = context.sender()
      val pong = (service ? s.toUpperCase).get[String].toLowerCase
      sender ! pong
  }
}

class PingController(val endpoint:ActorRef) extends Controller {
  implicit val executor = global

  override def apply(service:ActorRef):Unit = {
    get("/", Action { req =>
      Ok().sendView(Scalate("/templates/hello.jade", Map("title"->"Hello world!"))).build()
    })
    get("/ping", Action { req =>
      implicit val timeout = Timeout(3l, TimeUnit.SECONDS)
      val pong = (service ? "ping").get[String]
      Ok().sendView(Scalate("/templates/ping.jade", Map("pong" -> pong))).build()
    })
    get("/hello/:world", Action { req =>
      Ok().sendView(Scalate("/templates/world.jade", Map("world" -> req.params("world").getOrElse("failed")))).build()
    })
  }

  implicit def str2bytes(data:String):Array[Byte] = {
    data.getBytes("utf-8")
  }
}
