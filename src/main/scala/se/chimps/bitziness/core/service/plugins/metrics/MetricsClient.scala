package se.chimps.bitziness.core.service.plugins.metrics

import akka.actor.ActorRef
import se.chimps.bitziness.core.project.modules.metrics.service.domain.Metric
import se.chimps.bitziness.core.project.modules.metrics.service.domain.MetricTypes.SimpleCounter
import se.chimps.bitziness.core.service.Service
import se.chimps.bitziness.core.service.plugins.Plugin

/**
 * The metricsClient plugin. The plan is that services extends this trait and automatically know where to send their metrics.
 * Also there will be a few predefined metrics available.
 */
trait MetricsClient extends Plugin { me:Service =>
  val metricBuilder:MetricBuilder = Builder()
  val metricsServer:ActorRef
}

object Builder {
  def apply():MetricBuilder = new MetricBuilder {
  }
}

trait MetricBuilder {
  def forCounter(counter:Int):Metric = new SimpleCounter(counter)
}