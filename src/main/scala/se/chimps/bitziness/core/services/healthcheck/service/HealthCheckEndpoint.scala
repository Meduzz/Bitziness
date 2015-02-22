package se.chimps.bitziness.core.services.healthcheck.service

import akka.actor.ActorRef
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Action
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Framework.Controller
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Model.Responses.{Error, Ok}
import se.chimps.bitziness.core.endpoints.rest.{EndpointDefinition, RestEndpointBuilder, RESTEndpoint}
import se.chimps.bitziness.core.generic.Serializers.JSONSerializer
import se.chimps.bitziness.core.services.healthcheck.HealthChecks

/**
 *
 */
class HealthCheckEndpoint(val service:ActorRef) extends RESTEndpoint {
  override def rest: Receive = {
    case msg:Any => println(s"Unhandled message to HealthCheckEndpoint: ${msg}.")
  }

  override def configureRestEndpoint(builder: RestEndpointBuilder): EndpointDefinition = {
    builder.mountController("/admin", new HealthCheckController)
    builder.build()
  }
}

class HealthCheckController extends Controller with JSONSerializer {
  override def apply(endpoint: ActorRef): Unit = {
    get("/health", Action { req =>
      val health = HealthChecks.execute()
        .map {
          case (key, value) => if (value) { (key -> "healthy") } else { (key -> "unhealthy") }
        }

      Ok().sendEntity(toJSON(health), "application/javascript").build()
    })
    get("/ping", Action { req =>
      Ok().sendEntity("pong", "text/plain").build()
    })
  }
}
