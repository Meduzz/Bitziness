package example

import se.chimps.bitziness.core.BitzinessApp
import se.chimps.bitziness.core.services.healthcheck.service.HealthCheckService

object TestService extends BitzinessApp {
  override def initialize(args:Array[String]):Unit = {
    initService(classOf[PingService], "Ping")
    initService(classOf[HealthCheckService], "HealthChecks")
  }
}
