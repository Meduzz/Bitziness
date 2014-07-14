package se.chimps.bitziness.core.project.modules.metrics.service

import se.chimps.bitziness.core.service.AbstractService

/**
 * Metrics service, responsible for receiving metrics, aggregate them and publish them on endpoints.
 * This will be the REST endpoint, but we're waiting for akka-http to become stable first.
 */
class RestMetricsService(val host:String, val port:Int, val path:String) extends AbstractService /* with REST */ {

  // message will potentially come from 2 directions here.
  override def handle: Receive = rest

  def rest:Receive = {
    case _ => println("Camel was here, but left...")
  }
}
