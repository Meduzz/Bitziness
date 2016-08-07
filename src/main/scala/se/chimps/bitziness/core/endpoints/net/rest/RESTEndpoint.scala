package se.chimps.bitziness.core.endpoints.net.rest

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.event.Logging
import se.chimps.bitziness.core.endpoints.net.rest.spray.unrouting.Host
import se.chimps.bitziness.core.generic.ReceiveChain
import se.chimps.bitziness.core.Endpoint
import se.chimps.bitziness.core.endpoints.net.rest.spray.unrouting.Framework.Controller

import scala.concurrent.duration.FiniteDuration
import scala.util.{Try, Success, Failure}

/**
 * Main trait for RESTful endpoints.
 */
@deprecated
trait RESTEndpoint extends Endpoint with ReceiveChain {
  import scala.concurrent.ExecutionContext.Implicits.global

  private val log = Logging(context.system, getClass.getName)

  var restEndpoint:ActorRef = _

  def configureRestEndpoint(builder:RestEndpointBuilder):EndpointDefinition
  val definition = configureRestEndpoint(new RestEndpointBuilderImpl(self))

  def rest:Receive

  registerReceive(rest)

  override def receive:Receive = receives

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    Try(context.system.actorOf(Props(classOf[RestHandler], definition.host), definition.host.toString)) match {
      case Success(actor) => {
        restEndpoint = actor
        actor ! definition
      }
      case Failure(e) => {
        context.system
          .actorSelection(s"/user/${definition.host.toString}")
          .resolveOne(FiniteDuration(1L, TimeUnit.SECONDS)).onComplete {
          case Success(actor) => {
            restEndpoint = actor
            actor ! definition
          }
          case Failure(g: Throwable) => log.error("Found no actor, found an error instead.", g)
        }
      }
    }
  }
}

/**
 * Defaults to localhost:8080/
 */
sealed trait RestEndpointBuilder {
  def listen(host:String, port:Int):RestEndpointBuilder
  def mountController(path:String, module:Controller):RestEndpointBuilder
  def build():EndpointDefinition
}

private class RestEndpointBuilderImpl(val endpoint:ActorRef) extends RestEndpointBuilder {
  private var hostDefinition = new Host("0.0.0.0", 8080)
  private var routes:Map[String, Controller] = Map()

  override def listen(host:String, port:Int): RestEndpointBuilder = {
    hostDefinition = new Host(host, port)
    this
  }

  override def mountController(path:String, module:Controller):RestEndpointBuilder = {
    routes = routes ++ Map(path -> module)
    module(endpoint)
    this
  }

  override def build(): EndpointDefinition = {
    new EndpointDefinition(hostDefinition, routes)
  }
}

case class EndpointDefinition(host:Host, routes:Map[String, Controller])
