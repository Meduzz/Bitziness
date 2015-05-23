package example

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.event.Logging
import akka.util.Timeout
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Action
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Framework.Controller
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Model.Responses.Ok
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.view.{Jade4j, Scalate}
import se.chimps.bitziness.core.endpoints.rest.{EndpointDefinition, RestEndpointBuilder, RESTEndpoint}
import se.chimps.bitziness.core.generic.LocalSession.LocalSessionFactory
import se.chimps.bitziness.core.generic.{SessionFactory, SessionSupport}
import se.chimps.bitziness.core.generic.Waitable._
import se.chimps.bitziness.core.Service
import akka.pattern._
import scala.concurrent.ExecutionContext.global

class PingService extends Service {
  override def handle:Receive = {
    case "PING" => sender() ! "PONG"
  }

  override def initialize():Unit = {
    initEndpoint[PingEndpoint](classOf[PingEndpoint], "Http")
    healthCheck("PingService", () => 1==1)
  }
}

class PingEndpoint(val service:ActorRef) extends RESTEndpoint {
  implicit val executor = global
  val log = Logging(context.system, getClass.getName)

  override def configureRestEndpoint(builder:RestEndpointBuilder):EndpointDefinition = {
    healthCheck("PingEndpoint", () => 1==1)

    val controller = new PingController(self)
    builder.mountController("", controller)
    builder.build()
  }

  override def rest:Receive = {
    case s:String =>
      implicit val timeout = Timeout(3l, TimeUnit.SECONDS)
      val sender:ActorRef = context.sender()
      log.info("Pinging service")
      val pong = (service ? s.toUpperCase).get[String].toLowerCase
      sender ! pong
  }
}

class PingController(val endpoint:ActorRef) extends Controller with SessionSupport {
  implicit val executor = global

  override implicit val sessionFactory: SessionFactory = LocalSessionFactory

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
      Ok().sendView(Jade4j.classpath("templates/world.jade", Map("world" -> req.params("world").getOrElse("failed")))).build()
    })
    get("/cookie", Action { req =>
      val Seq(key, value, view, delete) = req.params("key", "value", "view", "delete")
      val cookie = req.cookie(view.getOrElse(""))

      var resp = Ok().sendView(Scalate("/templates/cookie.jade", Map("cookie" -> cookie.getOrElse(""), "key" -> view.getOrElse(""), "title" -> "Cookies")))

      if (key.isDefined && value.isDefined) {
        resp.cookie(key.get, value.get)
      }

      if (delete.isDefined) {
        resp.cookie(key = delete.get, value = "", expire = Some(1234L))
      }

      resp.build()
    })
  }

  implicit def str2bytes(data:String):Array[Byte] = {
    data.getBytes("utf-8")
  }
}
