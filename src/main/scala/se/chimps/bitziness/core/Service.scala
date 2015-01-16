package se.chimps.bitziness.core

import akka.actor.{Props, Actor, ActorRef}
import se.chimps.bitziness.core.generic.{ErrorMapping, ReceiveChain, HasFeature, Init}

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
}

/**
 * A base DSL for building projects. Individual modules should add their own dsl/builders/settings to this instance.
 */
object ServiceBuilder {
  def apply(projectRef:Service):ServiceBuilder = new ServiceBuilder {
  }
}

trait ServiceBuilder {
}