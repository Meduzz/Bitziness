package se.chimps.bitziness.core

import akka.actor.{Props, ActorRef, ActorSystem}
import se.chimps.bitziness.core.generic.Init

/**
 * Created by meduzz on 22/08/14.
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
}