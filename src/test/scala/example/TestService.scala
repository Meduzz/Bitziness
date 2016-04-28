package example

import se.chimps.bitziness.core.BitzinessApp
import se.chimps.bitziness.core.generic.ActorFactory
import se.chimps.bitziness.core.services.healthcheck.service.HealthCheckService
import se.chimps.bitziness.core.services.logging.adapters.ActorLoggingBacked

object TestService extends BitzinessApp {
  override def initialize(args:Array[String]):Unit = {
    initService(classOf[PingService], "Ping")
    initService(ActorFactory[HealthCheckService]("HealthChecks", () => new HealthCheckService("localhost", 8080, "/admin")))
    initService(classOf[ActorLoggingBacked], "LoggingService")
  }
}
