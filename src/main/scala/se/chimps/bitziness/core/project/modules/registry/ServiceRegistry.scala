package se.chimps.bitziness.core.project.modules.registry

import se.chimps.bitziness.core.generic.{Init, Event}
import se.chimps.bitziness.core.project.modules.events.Events
import se.chimps.bitziness.core.project.modules.Module
import se.chimps.bitziness.core.project.Project
import se.chimps.bitziness.core.service.Service
import akka.actor.{Props, ActorSystem, ActorRef}

/**
 * This module helps keeping track of registered services and are automatically extended by the
 * Project class.
 */
trait ServiceRegistry extends Module { project:Project =>
  def actorSystem:ActorSystem

  private var services:Seq[ActorRef] = Seq()

  def registerService[T<:Service](service:Class[T]) = {
    val actor = actorSystem.actorOf(Props(service), s"${service.getSimpleName}")
    services = services ++ Seq(actor)

    actor ! Init

    if (hasFeature[Events](project)) {
      project.asInstanceOf[Events].publish(new ServiceStarted(s"${service.getSimpleName}"))
    }
  }

  def getServices():Seq[ActorRef] = {
    services
  }
}

case class ServiceStarted(service:String) extends Event