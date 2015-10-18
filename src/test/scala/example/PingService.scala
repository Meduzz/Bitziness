package example

import java.util.concurrent.TimeUnit

import akka.actor.{ActorLogging, ActorRef}
import akka.http.javadsl.model.headers.SetCookie
import akka.http.scaladsl.model.{HttpResponse, DateTime}
import akka.http.scaladsl.model.headers.HttpCookie
import akka.stream.ActorMaterializer
import akka.util.{ByteString, Timeout}
import se.chimps.bitziness.core.endpoints.http.client.RequestBuilders
import se.chimps.bitziness.core.endpoints.http.server.unrouting.{ResponseBuilders, Action, Controller}
import se.chimps.bitziness.core.endpoints.http.{ConnectionBuilder, HttpClientEndpoint, HttpServerBuilder, HttpServerEndpoint}
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.view.{Jade4j, Scalate}
import se.chimps.bitziness.core.Service
import akka.pattern._
import se.chimps.bitziness.core.generic.logging.Log
import scala.concurrent.ExecutionContext.global
import scala.concurrent.Future

class PingService extends Service with Log {
  implicit val executor = global
  implicit val timeout = Timeout(3L, TimeUnit.SECONDS)

  var hts:ActorRef = _

  override def handle:Receive = {
    case "PING" => {
      info("Handling a PONG request.", Map("now" -> System.currentTimeMillis().toString))
      sender() ! "PONG"
    }
    case "HEALTHCHECK" => {
      info("Handling a HEALTHCHECK request.")
      val caller = sender()
      (hts ? "healthcheck").pipeTo(caller)
    }
  }

  override def initialize():Unit = {
    initEndpoint[PingEndpoint](classOf[PingEndpoint], "Http")
    hts = initEndpoint[HealthcheckEndpoint](classOf[HealthcheckEndpoint], "Healthchecks")
    healthCheck("PingService", () => 1==1)
  }
}

class PingEndpoint(val service:ActorRef) extends HttpServerEndpoint with Log {
  implicit val timeout = Timeout(3l, TimeUnit.SECONDS)
  implicit val materializer = ActorMaterializer()(context)

  override def createServer(builder:HttpServerBuilder):Future[ActorRef] = {
    info("Setting up the PingEndpoint.")

    healthCheck("PingEndpoint", () => 1==1)

    builder.listen("localhost", 9000).build()
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
  get("/healthcheck", Action { req =>
    (endpoint ? "healthcheck").mapTo[String].map(json => {
      Ok().withEntity(json)
    })
  })

  implicit def str2bytes(data:String):Array[Byte] = {
    data.getBytes("utf-8")
  }
}

/**
 * Fetches health checks from the /admin/health endpoint.
 * @param service
 */
class HealthcheckEndpoint(val service:ActorRef) extends HttpClientEndpoint with RequestBuilders with Log {
  implicit val executor = global
  implicit val timeout = Timeout(3L, TimeUnit.SECONDS)

  override def setupConnection(builder:ConnectionBuilder):ActorRef = {
    builder.host("localhost", 8080).build(false)
  }

  override def receive:Receive = {
    case "healthcheck" => {
      val caller = sender()
      debug("asking for health checks.")
      send(request("GET", "/admin/health")).pipeTo(caller)
    }
  }
}