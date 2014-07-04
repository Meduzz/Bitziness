package se.chimps.bitziness.core.project.modules.metrics.service.domain

/**
 * Contains all known metric types.
 */
object MetricTypes {
  case class SimpleCounter(value:Int) extends Metric {
    type T = Int
    val name = "SimpleCounter"
  }
}

trait Metric {
  type T
  def name:String
  def value:T
}