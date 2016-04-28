package example

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.http.javadsl.model.headers.SetCookie
import akka.http.scaladsl.model.headers.HttpCookie
import akka.http.scaladsl.model.{DateTime, HttpResponse}
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.util.{ByteString, Timeout}
import se.chimps.bitziness.core.Service
import se.chimps.bitziness.core.endpoints.http.server.unrouting.{Action, Controller, ResponseBuilders}
import se.chimps.bitziness.core.endpoints.http.{HttpServerBuilder, HttpServerEndpoint}
import se.chimps.bitziness.core.generic.HealthCheck
import se.chimps.bitziness.core.generic.logging.Log
import se.chimps.bitziness.core.generic.view.Jade4j

import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class PingService extends Service with Log with HealthCheck {
  implicit val executor = global
  implicit val timeout = Timeout(3L, TimeUnit.SECONDS)

  override def handle:Receive = {
    case "PING" => {
      info("Handling a PONG request.", Map("now" -> System.currentTimeMillis().toString))
      sender() ! "PONG"
    }
  }

  override def initialize():Unit = {
    initEndpoint[PingEndpoint](classOf[PingEndpoint], "Http")
    healthCheck("PingService", () => 1==1)
  }
}

class PingEndpoint(val service:ActorRef) extends HttpServerEndpoint with Log with HealthCheck {
  implicit val timeout = Timeout(3l, TimeUnit.SECONDS)
  implicit val materializer = ActorMaterializer()(context)
  import context.dispatcher

  override def createServer(builder:HttpServerBuilder):Future[ActorRef] = {
    info("Setting up the PingEndpoint.")

    builder.listen("localhost", 8080).build()
  }

  override def receive:Receive = {
    case s:String if "ping".equals(s) =>
      val caller = sender()
      info("Received a ping request.")
      (service ? s.toUpperCase).mapTo[String].map(_.toLowerCase).pipeTo(caller)
    case s:String =>
      val caller = sender()
      info(s"Received another request ($s).")
      (service ? s.toUpperCase).mapTo[HttpResponse].map(resp => {
        resp.entity.dataBytes.runFold(ByteString.empty)((base, in) => base.concat(in))
      }).map(fut => fut.map(_.utf8String.toLowerCase).pipeTo(caller))
  }

  @throws[Exception](classOf[Exception])
  override def preStart():Unit = {
    super.preStart()
    registerController(new PingController(self))

    healthCheck("PingEndpoint", () => 1==1)
  }
}

class PingController(val endpoint:ActorRef) extends Controller with ResponseBuilders {
  implicit val executor = global
  implicit val timeout = Timeout(3l, TimeUnit.SECONDS)

  get("/", Action.sync { req =>
    Ok().withView(Jade4j.classpath("templates/hello.jade", Map("title"->"Hello world!")))
  })
  get("/ping", Action { req =>
    val pong = (endpoint ? "ping").mapTo[String]
    pong.map(resp => Ok().withView(Jade4j.classpath("templates/ping.jade", Map("pong" -> resp))))
  })
  get("/hello/:world", Action.sync { req =>
    Ok().withView(Jade4j.classpath("templates/world.jade", Map("world" -> req.params.getOrElse("world", "failed"))))
  })
  get("/cookie", Action.sync { req =>
    val Seq(key, value, view, delete) = req.param("key", "value", "view", "delete")
    val cookie = req.cookie(view.getOrElse(""))

    var resp = Ok().withView(Jade4j.classpath("templates/cookie.jade", Map("cookie" -> cookie.getOrElse(""), "key" -> view.getOrElse(""), "title" -> "Cookies")))

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
  get("/healthcheck", Action { req =>
    (endpoint ? "healthcheck").mapTo[String].map(json => {
      Ok().withEntity(json)
    })
  })

  implicit def str2bytes(data:String):Array[Byte] = {
    data.getBytes("utf-8")
  }
}