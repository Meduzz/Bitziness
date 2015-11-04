package se.chimps.bitziness.core.generic.persistence.redis.endpoint

import se.chimps.bitziness.core.Endpoint
import se.chimps.bitziness.core.generic.persistence.redis.RedisFactory

trait RedisEndpoint extends Endpoint with RedisFactory
