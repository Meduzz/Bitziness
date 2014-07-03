package se.chimps.bitziness.core.project.modules.metrics

import se.chimps.bitziness.core.project.{ProjectBuilder, Project}
import se.chimps.bitziness.core.project.modules.Module

/**
 * Base trait for metrics module.
 * Endpoints for sharing metrics can be setup with the builder.
 * Number of Metrics listeners can also be set with the builder.
 */
trait Metrics extends Module { self:Project =>
  implicit def metricsBuilder(projectBuilder:ProjectBuilder) = new Builder(projectBuilder)
}

trait MetricsBuilder {
  def addMetric(metric:String) = println(metric)
}

class Builder(val projectBuilder:ProjectBuilder) {
  def setupMetrics():MetricsBuilder = new MetricsBuilder {}
}
