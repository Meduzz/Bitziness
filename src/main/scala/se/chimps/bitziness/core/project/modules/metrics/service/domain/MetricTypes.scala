package se.chimps.bitziness.core.project.modules.metrics.service.domain

/**
 * Contains all known metric types.
 */
object MetricTypes {
  case class SimpleCounter(value:Int) extends Metric {
    val name = "SimpleCounter"
  }
}

trait Metric {
  def name;
  def value;
}