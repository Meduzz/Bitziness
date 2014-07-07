package se.chimps.bitziness.core.project.modules.registry

import se.chimps.bitziness.core.project.modules.Module
import se.chimps.bitziness.core.project.{ProjectBuilder, Project}
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
    services = services ++ Seq(actorSystem.actorOf(Props(service), s"/project/services/${service.getSimpleName}"))
    // TODO broadcast event of the new service.
  }

  implicit def registryToProjectHook(builder:ProjectBuilder):Builder = {
    return new Builder {
      override def addService[T <: Service](service:Class[T]):ProjectBuilder = {
        project.registerService(service)
        builder
      }
    }
  }
}

trait Builder {
  def addService[T<:Service](service:Class[T]):ProjectBuilder
}