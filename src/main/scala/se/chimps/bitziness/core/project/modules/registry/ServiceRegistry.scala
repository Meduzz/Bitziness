package se.chimps.bitziness.core.project.modules.registry

import se.chimps.bitziness.core.project.modules.Module
import se.chimps.bitziness.core.project.Project
import se.chimps.bitziness.core.service.Service
import akka.actor.{Props, ActorSystem, ActorRef}

/**
 * Created by meduzz on 05/07/14.
 */
trait ServiceRegistry extends Module { project:Project =>
  def actorSystem:ActorSystem

  var services:Seq[ActorRef] = Seq()

  def registerService[T<:Service](service:Class[T]) = {
    services = services ++ Seq(actorSystem.actorOf(Props(service), service.getName))
    // TODO broadcast event of the new service.
  }

  // TODO def broadcast(event:Event) sends event to all services.
}
