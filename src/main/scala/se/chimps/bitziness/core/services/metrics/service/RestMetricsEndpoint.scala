package se.chimps.bitziness.core.project.modules.metrics.service

import akka.actor.ActorRef
import se.chimps.bitziness.core.endpoints.rest.{EndpointDefinition, RestEndpointBuilder, RESTEndpoint}

/**
 * Metrics service, responsible for receiving metrics, aggregate them and publish them on endpoints.
 */
class RestMetricsEndpoint(val service:ActorRef) extends RESTEndpoint {
  override def configureRestEndpoint(builder:RestEndpointBuilder):EndpointDefinition = {
    builder.build()
  }

  override def receive:Receive = {
    case _ =>
  }
}
