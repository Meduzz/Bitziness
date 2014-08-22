package se.chimps.bitziness.core.project.modules.metrics.service

import akka.actor.Actor.Receive
import se.chimps.bitziness.core.endpoints.rest.{EndpointDefinition, RestEndpointBuilder, RESTEndpoint}

/**
 * Metrics service, responsible for receiving metrics, aggregate them and publish them on endpoints.
 */
class RestMetricsEndpoint(val host:String, val port:Int, val path:String) extends RESTEndpoint {
  override def configureRestEndpoint(builder:RestEndpointBuilder):EndpointDefinition = {
    builder.build()
  }

  override def receive:Receive = {
    case _ =>
  }
}
