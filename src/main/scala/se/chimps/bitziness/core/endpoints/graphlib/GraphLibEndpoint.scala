package se.chimps.bitziness.core.endpoints.graphlib

import se.chimps.bitziness.core.generic.persistence.redis.endpoint.RedisEndpoint
import se.kodiak.tools.graphs.Graph
import se.kodiak.tools.graphs.graphsources.RedisGraphSource

trait GraphLibEndpoint extends RedisEndpoint {

  def graph(name:String):Graph = {
    Graph(RedisGraphSource(redis, name))
  }

}
