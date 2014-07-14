package se.chimps.bitziness.core.project.modules.registry

import se.chimps.bitziness.core.generic.Event
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
    services = services ++ Seq(actorSystem.actorOf(Props(service), s"${service.getSimpleName}"))

    if (hasFeature[Events](project)) {
      project.asInstanceOf[Events].publish(new ServiceStarted(s"${service.getSimpleName}"))
    }
  }

  def getServices():Seq[ActorRef] = {
    services
  }
}

case class ServiceStarted(service:String) extends Event