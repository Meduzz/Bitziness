package example

import akka.actor.{ActorRef, Actor}
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Action
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Framework.Controller
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Model.Responses.Ok
import se.chimps.bitziness.core.endpoints.rest.{EndpointDefinition, RestEndpointBuilder, RESTEndpoint}
import se.chimps.bitziness.core.{Service}

class PingService extends Service {
  override def handle:Receive = {
    case _ => println("Ping")
  }

  override def initialize():Unit = {
    initEndpoint[PingEndpoint](classOf[PingEndpoint], "Http")
  }
}

class PingEndpoint extends RESTEndpoint {
  override def configureRestEndpoint(builder:RestEndpointBuilder):EndpointDefinition = {
    val controller = new PingController
    builder.mountController("", controller)
    builder.build()
  }

  override def receive:Actor.Receive = {
    case _ => println("Ping")
  }
}

class PingController extends Controller {
  override def apply(service:ActorRef):Unit = {
    get("/", Action { req =>
      Ok().build()
    })
    get("/hello/:world", Action { req =>
      Ok().withEntity(s"Hello ${req.params("world").getOrElse("failed")}!").build()
    })
  }

  implicit def str2bytes(data:String):Array[Byte] = {
    data.getBytes("utf-8")
  }
}
