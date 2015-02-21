package se.chimps.bitziness.core

import akka.actor.{Props, ActorRef, ActorSystem}
import se.chimps.bitziness.core.generic.{Naming, ActorFactory, Init}
import se.chimps.bitziness.core.services.healthcheck.HealthChecks

import scala.reflect.ClassTag

/**
 *
 */
abstract class BitzinessApp extends Bitziness {
  def main(args:Array[String]) = {
    initialize(args)
  }
}

trait Bitziness {
  val system = ActorSystem()

  def initialize(args:Array[String]):Unit

  def initService[T<:Service](service:Class[T], name:String):ActorRef = {
    val actor = system.actorOf(Props(service), name)
    actor ! Init
    actor
  }

  def initService[T<:Service](service:Class[T], name:String, args:Seq[Any]):ActorRef = {
    val actor = system.actorOf(Props(service, args), name)
    actor ! Init
    actor
  }

  def initService(factory:ActorFactory[Service]):ActorRef = {
    val actor = factory.actor()
    actor ! Init
    actor
  }

  def initService[T<:Service with Naming](service:T)(implicit tag:ClassTag[T]):ActorRef = {
    val actor = system.actorOf(Props(service), service.name())
    actor ! Init
    actor
  }

  def healthCheck(name:String, hc:()=>Boolean):Unit = HealthChecks.register(name, hc)
}