package se.chimps.bitziness.core.service.plugins.rest

import akka.actor.{Props, ActorSystem, ActorRef}
import akka.io.IO
import se.chimps.bitziness.core.service.Service
import se.chimps.bitziness.core.service.plugins.Plugin
import se.chimps.bitziness.core.service.plugins.rest.unrouting.Spray.Module
import spray.can.Http

/**
 * Main trait for RESTful endpoints. Waiting for Akka-http to get stable before continuing with this.
 */
trait REST extends Plugin { me:Service =>

  def configureRestEndpoint(builder:RestEndpointBuilder):EndpointDefinition

  val restEndpoint = SetupRestEndpoint(configureRestEndpoint)(context.system)(new RestEndpointBuilderImpl(me.self))
}

/**
 * Defaults to localhost:8080/
 */
sealed trait RestEndpointBuilder {
  def listen(host:String, port:Int):RestEndpointBuilder
  def registerEndpoint(module:Module):RestEndpointBuilder
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
  private var modules:Seq[Module] = Seq()

  override def listen(host:String, port:Int): RestEndpointBuilder = {
    hostDefinition = new Host(host, port)
    this
  }

  override def registerEndpoint(module:Module):RestEndpointBuilder = {
    modules = modules ++ Seq(module)
    this
  }

  override def build(): EndpointDefinition = {
    new EndpointDefinition(hostDefinition, service, modules)
  }
}

case class Host(host:String, port:Int) {
  override def toString: String = s"${host}:${port}"
}

case class EndpointDefinition(hostDef:Host, service:ActorRef, modules:Seq[Module])

trait SetupRestEndpoint extends (RestEndpointBuilder=>ActorRef) {
  def apply(builder:RestEndpointBuilder):ActorRef
}

object SetupRestEndpoint {
  def apply(m:(RestEndpointBuilder)=>EndpointDefinition)(implicit system:ActorSystem):SetupRestEndpoint = new SetupRestEndpoint {
    override def apply(builder: RestEndpointBuilder): ActorRef = {
      val definition = m(builder)
      EndpointDefinitionHolder.getOrElse(definition.hostDef.toString, definition)
      val handler = system.actorOf(Props(classOf[RestHandler]))
      handler ! definition

      IO(Http) ! Http.Bind(handler, definition.hostDef.host, definition.hostDef.port)

      handler
    }
  }
}