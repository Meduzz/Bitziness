package se.chimps.bitziness.core.project

import akka.actor.ActorSystem
import se.chimps.bitziness.core.project.modules.registry.ServiceRegistry
import se.chimps.bitziness.core.generic.{Init, HasFeature}

/**
 * This is the first class that will be extended.
 * A project contains the main-method that will be started by the jvm. It can be enchanced further with features like
 * metrics etc. Each project are supposed to have multiple services, the other way around will work, but will be messy to deploy.
 */
abstract class AbstractProject extends Project {

  val projectBuilder = ProjectBuilder(this)
  val actorSystem = ActorSystem("bitziness")

  def main(args:Array[String]) = {
    initialize(args)
  }
}

trait Project extends ServiceRegistry with HasFeature {

  def initialize(args:Array[String]):Unit

}