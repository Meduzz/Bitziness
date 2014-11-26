package se.chimps.bitziness.core.endpoints.camel

import akka.camel.{Producer, Consumer}
import se.chimps.bitziness.core.Endpoint

/*
 * Well this got more than a bit pointless...
 */

/**
 * Base trait for a consuming CamelEndpoint.
 */
trait CamelConsumerEndpoint extends Consumer with Endpoint {

}

/**
 * Base trait for a producing CamelEndpoint.
 */
trait CamelProducerEndpoint extends Producer with Endpoint {

}