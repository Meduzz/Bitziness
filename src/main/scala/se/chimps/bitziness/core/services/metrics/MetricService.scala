package se.chimps.bitziness.core.services.metrics

import se.chimps.bitziness.core.Service
import se.chimps.bitziness.core.generic.Events
import se.chimps.bitziness.core.generic.metrics._

trait MetricService extends Service with Events with Adapter {
  override def handle:Receive = {
    case Counter(name, value) => counter(name, value)
    case Gauge(name, value) => gauge(name, value)
    case Histogram(name, value) => histogram(name, value)
    case Meter(name, value, timestamp) => meter(name, value, timestamp)
    case Timer(name, value) => timer(name, value)
  }

  override def initialize():Unit = {
    this.internalEventsBuilder.subscribe(classOf[Metric[_]])
  }
}

trait Adapter {
  def counter(name:String, value:Long)
  def gauge(name:String, value:Long)
  def histogram(name:String, value:BigDecimal)
  def meter(name:String, value: BigDecimal, timestamp:Long)
  def timer(name:String, value:Long)
}