package example

import java.util.concurrent.TimeUnit

import akka.actor.{ActorLogging, ActorRef}
import akka.http.javadsl.model.headers.SetCookie
import akka.http.scaladsl.model.DateTime
import akka.http.scaladsl.model.headers.HttpCookie
import akka.util.Timeout
import se.chimps.bitziness.core.endpoints.http.server.unrouting.{ResponseBuilders, Action, Controller}
import se.chimps.bitziness.core.endpoints.http.{HttpServerBuilder, HttpServerEndpoint}
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.view.{Jade4j, Scalate}
import se.chimps.bitziness.core.Service
import akka.pattern._
import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class PingService extends Service {
  override def handle:Receive = {
    case "PING" => sender() ! "PONG"
  }

  override def initialize():Unit = {
    initEndpoint[PingEndpoint](classOf[PingEndpoint], "Http")
    healthCheck("PingService", () => 1==1)
  }
}

class PingEndpoint(val service:ActorRef) extends HttpServerEndpoint with ActorLogging {
  override def createServer(builder:HttpServerBuilder):Future[ActorRef] = {
    healthCheck("PingEndpoint", () => 1==1)

    builder.listen("localhost", 9000).build()
  }

  override def receive:Receive = {
    case s:String =>
      implicit val timeout = Timeout(3l, TimeUnit.SECONDS)
      val sender:ActorRef = context.sender()
      log.info("Pinging service")
      val pong = (service ? s.toUpperCase).mapTo[String].map(_.toLowerCase).pipeTo(sender)
  }

  @throws[Exception](classOf[Exception])
  override def preStart():Unit = {
    super.preStart()
    registerController(new PingController(self))
  }
}

class PingController(val endpoint:ActorRef) extends Controller with ResponseBuilders {
  implicit val executor = global
  implicit val timeout = Timeout(3l, TimeUnit.SECONDS)

  get("/", Action.sync { req =>
    Ok().withView(Scalate("/templates/hello.jade", Map("title"->"Hello world!")))
  })
  get("/ping", Action { req =>
    val pong = (endpoint ? "ping").mapTo[String]
    pong.map(resp => Ok().withView(Scalate("/templates/ping.jade", Map("pong" -> resp))))
  })
  get("/hello/:world", Action.sync { req =>
    Ok().withView(Jade4j.classpath("templates/world.jade", Map("world" -> req.params.getOrElse("world", "failed"))))
  })
  get("/cookie", Action.sync { req =>
    val Seq(key, value, view, delete) = req.param("key", "value", "view", "delete")
    val cookie = req.cookie(view.getOrElse(""))

    var resp = Ok().withView(Scalate("/templates/cookie.jade", Map("cookie" -> cookie.getOrElse(""), "key" -> view.getOrElse(""), "title" -> "Cookies")))

    if (key.isDefined && value.isDefined) {
      resp = resp.withHeaders(SetCookie.create(HttpCookie(key.get, value.get)))
    }

    if (delete.isDefined) {
      resp = resp.withHeaders(SetCookie.create(HttpCookie(delete.get, "", Some(DateTime(1234L)))))
    }

    resp
  })
  get("/form", Action.sync { req =>
    Ok().withView(Jade4j.classpath("templates/form.jade", Map()))
  })
  post("/form", Action { req =>
    req.asFormData().map {map =>
      Ok().withView(Jade4j.classpath("templates/form.jade", Map("resp" -> s"${map("key")} ${map("value")}")))
    }
  })

  implicit def str2bytes(data:String):Array[Byte] = {
    data.getBytes("utf-8")
  }
}
