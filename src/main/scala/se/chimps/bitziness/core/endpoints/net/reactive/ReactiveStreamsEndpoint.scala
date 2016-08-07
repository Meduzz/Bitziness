package se.chimps.bitziness.core.endpoints.net.reactive

import akka.stream.scaladsl.RunnableGraph
import se.chimps.bitziness.core.Endpoint

/**
 *
 */
trait ReactiveStreamsEndpoint[T] extends Endpoint {
  def setupGraph():RunnableGraph[T]

  val graph = setupGraph()
}