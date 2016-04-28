package se.chimps.bitziness.core.services.healthcheck.service

import akka.actor.ActorRef
import akka.http.scaladsl.model.ContentTypes
import se.chimps.bitziness.core.Service
import se.chimps.bitziness.core.endpoints.http.server.unrouting.{Action, Controller, ResponseBuilders}
import se.chimps.bitziness.core.endpoints.http.{HttpServerBuilder, HttpServerEndpoint}
import se.chimps.bitziness.core.generic.Serializers.JSONSerializer
import se.chimps.bitziness.core.services.healthcheck.HealthChecks

import scala.concurrent.{ExecutionContext, Future}

/**
 *
 */
class HealthCheckService(val host:String, val port:Int, val adminPath:String) extends Service with HttpServerEndpoint {

	override def createServer(builder: HttpServerBuilder): Future[ActorRef] = {
		builder.listen(host, port).build()
	}

	override def handle: Receive = {
    case msg:Any => println(s"Unhandled message received by HealthCheckService: ${msg}.")
  }

  override def initialize(): Unit = {
		registerController(new HealthCheckController(adminPath)(context.dispatcher))
  }
}

class HealthCheckController(val adminPath:String)(implicit ec:ExecutionContext) extends Controller with JSONSerializer with ResponseBuilders {
	get(s"$adminPath/health", Action.sync { req =>
		val health = HealthChecks.execute()
			.map {
				case (key, value) => if (value) { (key -> "healthy") } else { (key -> "unhealthy") }
			}

		Ok().withEntity(ContentTypes.`application/json`, toJSON(health))
	})
	get(s"$adminPath/ping", Action.sync { req =>
		Ok().withEntity(ContentTypes.`text/plain(UTF-8)`, "pong")
	})
}
