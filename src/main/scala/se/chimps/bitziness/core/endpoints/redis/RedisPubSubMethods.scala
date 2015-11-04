package se.chimps.bitziness.core.endpoints.redis

/**
 * Adds convenience method to RedisPubSub.
 */
trait RedisPubSubMethods { endpoint:RedisPubSubEndpoint =>

  def publish(channel:String, data:String) = {
    redis.publish(channel, data)
  }

}
