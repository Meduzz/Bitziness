package example

import akka.event.Logging
import se.chimps.bitziness.core.BitzinessApp
import se.chimps.bitziness.core.generic.ActorFactory
import se.chimps.bitziness.core.services.healthcheck.service.HealthCheckService
import se.chimps.bitziness.core.services.logging.LoggingService
import se.chimps.bitziness.core.services.logging.delegates.ActorLoggingDelegate

object TestService extends BitzinessApp {
  override def initialize(args:Array[String]):Unit = {
    initService(classOf[PingService], "Ping")
    initService(ActorFactory[HealthCheckService]("HealthChecks", () => new HealthCheckService("localhost", 8080, "/admin")))
    initService(ActorFactory[LoggingService]("LoggingService", () => new LoggingService(new ActorLoggingDelegate(Logging(system, "TestService")))))
  }
}
