package example

import se.chimps.bitziness.core.BitzinessApp
import se.chimps.bitziness.core.services.healthcheck.service.HealthCheckService
import se.chimps.bitziness.core.services.logging.LoggingService
import se.chimps.bitziness.core.services.logging.adapters.ActorLoggingBacked

object TestService extends BitzinessApp {
  override def initialize(args:Array[String]):Unit = {
    initService(classOf[PingService], "Ping")
    initService(classOf[HealthCheckService], "HealthChecks")
    initService(classOf[ActorLoggingBacked], "LoggingService")
  }
}
