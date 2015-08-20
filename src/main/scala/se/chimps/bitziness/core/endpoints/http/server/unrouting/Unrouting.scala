package se.chimps.bitziness.core.endpoints.http.server.unrouting

import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.stream.Materializer
import se.chimps.bitziness.core.endpoints.http.HttpServerBuilder

import scala.concurrent.Future

/**
 *
 */
trait Unrouting extends ActionRegistry with Engine {
  implicit def materializer:Materializer

  def requestHandler:(HttpRequest) => Future[HttpResponse] = {
    case request:HttpRequest => handleRequest(request)
  }

  implicit def enhanceServerBuilder(builder:HttpServerBuilder):EnhancedServerBuilder = new EnhancedServerBuilder(builder, this)
}

class EnhancedServerBuilder(builder:HttpServerBuilder, registry:ActionRegistry) {

  def registerController(controller:Controller):HttpServerBuilder = {
    registry.registerController(controller)
    builder
  }
}