package se.chimps.bitziness.core.service.plugins.rest

import akka.actor.ActorRef
import se.chimps.bitziness.core.service.Service
import se.chimps.bitziness.core.service.plugins.Plugin

/**
 * Main trait for RESTful endpoints. Waiting for Akka-http to get stable before continuing with this.
 */
trait REST extends Plugin { me:Service =>

  def configureEndpoint(builder:RestEndpointBuilder):RestEndpointBuilder

  val endpoint = configureEndpoint(new RestEndpointBuilderImpl(self))
}

/**
 * Defaults to localhost:8080/
 */
sealed trait RestEndpointBuilder {
  def host(host:String):RestEndpointBuilder
  def port(port:Int):RestEndpointBuilder
  def basePath(path:String):RestEndpointBuilder
  def addRoute(path:String, handler:(Request)=>Response):RestEndpointBuilder
}

private class RestEndpointBuilderImpl(val handler:ActorRef) extends RestEndpointBuilder {
  private var host:String = "localhost"
  private var port:Int = 8080
  private var base:String = "/"
  private var routes:Map[String, (Request)=>Response] = Map()

  override def host(host: String): RestEndpointBuilder = {
    this.host = host
    this
  }

  override def basePath(path: String): RestEndpointBuilder = {
    this.base = path
    this
  }

  override def port(port: Int): RestEndpointBuilder = {
    this.port = port
    this
  }

  override def addRoute(path: String, handler: (Request) => Response): RestEndpointBuilder = {
    routes += (path -> handler)
    this
  }
}

case class Request(path:String, headers:Map[String, Any], body:Any)
case class Response(headers:Map[String, Any], body:Any)

case class Method(method:String) {
  val GET = Method("GET")
  val POST = Method("POST")
  val PUT = Method("PUT")
  val DELETE = Method("DELETE")
}