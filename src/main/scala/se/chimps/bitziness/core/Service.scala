package se.chimps.bitziness.core

import akka.actor.{Props, Actor, ActorRef}
import se.chimps.bitziness.core.generic._
import se.chimps.bitziness.core.services.healthcheck.HealthChecks

import scala.reflect.ClassTag

abstract class Service extends Actor with HasFeature with ReceiveChain with ErrorMapping {
  def handle:Receive

  private def init:Receive = {
    case Init => initialize()
  }

  registerReceive(init)
  registerReceive(handle)

  override def receive:Receive = receives

  def initialize():Unit

  def initEndpoint[T<:Endpoint](endpoint:Class[T], name:String):ActorRef = {
    context.system.actorOf(Props(endpoint, self), name)
  }

  def initEndpoint[T<:Endpoint](endpoint:T, name:String)(implicit evidence:ClassTag[T]):ActorRef = {
    context.system.actorOf(Props(endpoint), name)
  }

  def initEndpoint[T<:Endpoint with Naming](endpoint:T)(implicit tag:ClassTag[T]):ActorRef = {
    context.system.actorOf(Props(endpoint), endpoint.name())
  }

  def initEndpoint(factory:ActorFactory[Endpoint]):ActorRef = {
    factory.actor()
  }

  def healthCheck(name:String, check:()=>Boolean):Unit = HealthChecks.register(name, check)
}