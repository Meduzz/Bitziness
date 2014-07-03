package se.chimps.bitziness.core.project

import se.chimps.bitziness.core.service.Service

/**
 * A base DSL for building projects. Individual modules should add their own dsl/builders/settings to this instance.
 */
object ProjectBuilder {
  def apply():ProjectBuilder = new ProjectBuilder {
    override def addService[T<:Service](service: Class[T]): ProjectBuilder = { println(s"ProjectBuilder got ${service.getName}"); this }
  }
}

trait ProjectBuilder {
  def addService[T<:Service](service:Class[T]):ProjectBuilder
}