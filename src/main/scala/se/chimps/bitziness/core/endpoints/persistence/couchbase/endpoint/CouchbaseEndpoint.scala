package se.chimps.bitziness.core.endpoints.persistence.couchbase.endpoint

import se.chimps.bitziness.core.Endpoint
import se.chimps.bitziness.core.endpoints.persistence.couchbase.CouchbaseFactory

trait CouchbaseEndpoint extends Endpoint with CouchbaseFactory
