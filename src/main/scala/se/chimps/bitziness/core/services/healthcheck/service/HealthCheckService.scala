package se.chimps.bitziness.core.services.healthcheck.service

import se.chimps.bitziness.core.Service

/**
 *
 */
class HealthCheckService extends Service {

  override def handle: Receive = {
    case msg:Any => println(s"Unhandled message received by HealthCheckService: ${msg}.")
  }

  override def initialize(): Unit = {
    initEndpoint(classOf[HealthCheckEndpoint], "HealthCheckEndpoint")
  }
}
