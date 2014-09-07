package se.chimps.bitziness.core.endpoints.rest

import _root_.spray.can.Http
import akka.actor.{Actor, Props, ActorSystem, ActorRef}
import akka.io.IO
import se.chimps.bitziness.core.Endpoint
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Framework.{Controller}

/**
 * Main trait for RESTful endpoints.
 */
trait RESTEndpoint extends Actor with Endpoint {

  def configureRestEndpoint(builder:RestEndpointBuilder):EndpointDefinition

  val restEndpoint = SetupRestEndpoint(configureRestEndpoint)(context.system)(new RestEndpointBuilderImpl(self))
}

/**
 * Defaults to localhost:8080/
 */
sealed trait RestEndpointBuilder {
  def listen(host:String, port:Int):RestEndpointBuilder
  def mountController(path:String, module:Controller):RestEndpointBuilder
  def build():EndpointDefinition
}

object EndpointDefinitionHolder {
  private var endpoints = Map[String, EndpointDefinition]()

  def getOrElse(key:String, impl:EndpointDefinition):EndpointDefinition = {
    val value = endpoints.getOrElse(key, impl)
    if (value.equals(impl)) {
      put(key, impl)
    }
    value
  }

  def put(key:String, impl:EndpointDefinition) = {
    endpoints = endpoints + (key -> impl)
  }
}

private class RestEndpointBuilderImpl(val service:ActorRef) extends RestEndpointBuilder {
  private var hostDefinition = new Host("localhost", 8080)
  private var modules:Map[String, Controller] = Map()

  override def listen(host:String, port:Int): RestEndpointBuilder = {
    hostDefinition = new Host(host, port)
    this
  }

  override def mountController(path:String, module:Controller):RestEndpointBuilder = {
    modules = modules ++ Map(path -> module)
    module(service)
    this
  }

  override def build(): EndpointDefinition = {
    new EndpointDefinition(hostDefinition, service, modules)
  }
}

case class Host(host:String, port:Int) {
  override def toString: String = s"${host}:${port}"
}

case class EndpointDefinition(host:Host, service:ActorRef, modules:Map[String, Controller])

trait SetupRestEndpoint extends (RestEndpointBuilder=>ActorRef) {
  def apply(builder:RestEndpointBuilder):ActorRef
}

object SetupRestEndpoint {
  def apply(m:(RestEndpointBuilder)=>EndpointDefinition)(implicit system:ActorSystem):SetupRestEndpoint = new SetupRestEndpoint {
    override def apply(builder: RestEndpointBuilder): ActorRef = {
      val definition = m(builder)
      EndpointDefinitionHolder.getOrElse(definition.host.toString, definition)
      val handler = system.actorOf(Props(classOf[RestHandler], definition))

      IO(Http) ! Http.Bind(handler, definition.host.host, definition.host.port)

      handler
    }
  }
}