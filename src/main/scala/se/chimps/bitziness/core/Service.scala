package se.chimps.bitziness.core

import akka.actor.{Actor, ActorRef, Props}
import se.chimps.bitziness.core.generic._

import scala.reflect.ClassTag

abstract class Service extends Actor with HasFeature with ReceiveChain {
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

  def initEndpoint[T<:Endpoint](factory:ActorFactory[T])(implicit tag:ClassTag[T]):ActorRef = {
    context.system.actorOf(Props(factory.actor), factory.name)
  }
}