package se.chimps.bitziness.core.project.modules.metrics

import se.chimps.bitziness.core.Service

object Metrics {

  class MetricsService extends Service {
    override def handle: Receive = {
      case _ =>
    }

    override def initialize(): Unit = {

    }
  }

}