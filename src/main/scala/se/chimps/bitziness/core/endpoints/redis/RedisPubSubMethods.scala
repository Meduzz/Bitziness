package se.chimps.bitziness.core.endpoints.redis

/**
 * Adds convenience method to RedisPubSub.
 */
trait RedisPubSubMethods { endpoint:RedisPubSubTrait =>

  def publish(channel:String, data:String) = {
    redis.publish(channel, data)
  }

}
