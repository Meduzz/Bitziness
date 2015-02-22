package se.chimps.bitziness.core.endpoints.rest

import java.util.concurrent.TimeUnit

import akka.actor._
import org.slf4j.LoggerFactory
import se.chimps.bitziness.core.generic.{ActorLock, ReceiveChain}
import se.chimps.bitziness.core.{Host, Endpoint}
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Framework.{Controller}

import scala.concurrent.duration.FiniteDuration
import scala.util.{Success, Failure}

/**
 * Main trait for RESTful endpoints.
 */
trait RESTEndpoint extends Endpoint with ReceiveChain {
  import scala.concurrent.ExecutionContext.Implicits.global

  private val log = LoggerFactory.getLogger(getClass.getName)

  var restEndpoint:ActorRef = _

  def configureRestEndpoint(builder:RestEndpointBuilder):EndpointDefinition
  val definition = configureRestEndpoint(new RestEndpointBuilderImpl(self))

  def rest:Receive

  def setup:Receive = {
    case ActorIdentity(id, Some(actor)) => restEndpoint = actor; actor ! Routes(definition.routes)
    case ActorIdentity(id, None) => restEndpoint = context.system.actorOf(Props(classOf[RestHandler], definition.host), definition.host.toString); restEndpoint ! Routes(definition.routes)
  }

  registerReceive(rest)
  registerReceive(setup)

  override def receive:Receive = receives

  ActorLock(definition.host.toString) match {
    case Some(bool) => {
      log.trace("Missed lock, waiting...")
      while (bool.get) {
        log.trace("..wating")
      }
      log.trace(".. received lock.")
      context.system
        .actorSelection(s"/user/${definition.host.toString}")
        .resolveOne(FiniteDuration(1L, TimeUnit.SECONDS)).onComplete {
          case Success(actor) => {
            restEndpoint = actor
            actor ! Routes(definition.routes)
          }
          case Failure(e:Throwable) => throw e
      }
    }
    case None => {
      log.trace("Got lock.")
      restEndpoint = context.system.actorOf(Props(classOf[RestHandler], definition.host), definition.host.toString)
      val lock = ActorLock.get(definition.host.toString)
      log.trace(".. returning lock.")
      while (!lock.compareAndSet(true, false)) {
        log.trace("failed, retrying")
      }
      log.trace("lock returned")
      restEndpoint ! Routes(definition.routes)
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
case class Routes(routes:Map[String, Controller])

trait SetupRestEndpoint extends (RestEndpointBuilder=>ActorRef) {
  def apply(builder:RestEndpointBuilder):ActorRef
}

object SetupRestEndpoint {
  def apply(m:(RestEndpointBuilder)=>EndpointDefinition)(implicit system:ActorSystem):SetupRestEndpoint = new SetupRestEndpoint {
    override def apply(builder: RestEndpointBuilder): ActorRef = {
      val definition = m(builder)

      val handler = system.actorOf(Props(classOf[RestHandler], definition.host), definition.host.toString)

      handler ! Routes(definition.routes)

      handler
    }
  }
}