package se.chimps.bitziness.core.project

import akka.actor.ActorSystem
import se.chimps.bitziness.core.project.modules.registry.ServiceRegistry
import se.chimps.bitziness.core.generic.HasFeature

/**
 * This is the first class that will be extended.
 * A project contains the main-method that will be started by the jvm. It can be enchanced further with features like
 * metrics etc. Each project are supposed to have multiple services, the other way around will work, but will be messy to deploy.
 */
abstract class Project extends ServiceRegistry with HasFeature {

  val projectBuilder = ProjectBuilder(this)
  val actorSystem = ActorSystem("bitziness")

  def initialize(args:Array[String]):Unit

  def main(args:Array[String]) = {
    initialize(args)
  }
}
