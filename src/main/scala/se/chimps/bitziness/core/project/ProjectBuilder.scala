package se.chimps.bitziness.core.project

/**
 * A base DSL for building projects. Individual modules should add their own dsl/builders/settings to this instance.
 */
object ProjectBuilder {
  def apply(projectRef:Project):ProjectBuilder = new ProjectBuilder {
  }
}

trait ProjectBuilder {
}