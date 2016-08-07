package se.chimps.bitziness.core.endpoints.persistence.redis.endpoint

import se.chimps.bitziness.core.Endpoint
import se.chimps.bitziness.core.endpoints.persistence.redis.RedisFactory

trait RedisEndpoint extends Endpoint with RedisFactory
