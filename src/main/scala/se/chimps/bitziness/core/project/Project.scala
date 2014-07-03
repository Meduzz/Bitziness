package se.chimps.bitziness.core.project

/**
 * This is the first class that will be extended.
 * A project contains the main-method that will be started by the jvm. It can be enchanced further with features like
 * metrics etc. Each project are supposed to have multiple services, the other way around will work, but will be messy to deploy.
 */
abstract class Project {

  val projectBuilder = ProjectBuilder()

  def initialize(args:Array[String]):Unit

  def main(args:Array[String]) = {
    initialize(args)
  }
}
