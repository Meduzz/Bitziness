package se.chimps.bitziness.core.endpoints.http.server.unrouting

import java.net.InetSocketAddress

import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.stream.Materializer

import scala.concurrent.{ExecutionContext, Future}

/**
 *
 */
trait Unrouting extends ActionRegistry with Engine {
  implicit def materializer:Materializer
  implicit def ec:ExecutionContext

  def requestHandler(inet:InetSocketAddress):(HttpRequest) => Future[HttpResponse] = {
    case request:HttpRequest => handleRequest(inet, request)
  }
}