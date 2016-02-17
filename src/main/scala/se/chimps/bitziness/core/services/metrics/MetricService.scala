package se.chimps.bitziness.core.services.metrics

import se.chimps.bitziness.core.Service
import se.chimps.bitziness.core.generic.Events
import se.chimps.bitziness.core.generic.metrics._

trait MetricService extends Service with Events with Adapter {

  // TODO add optional timestamp to all metrics.

  def host:String

  override def handle:Receive = {
    case LongMetric(long, service, name, state, metadata) => metric(host, service, name, long, state, metadata)
    case DecimalMetric(deci, service, name, state, metadata) => metric(host, service, name, deci, state, metadata)
    case BooleanMetric(bool, service, name, state, metadata) => metric(host, service, name, bool, state, metadata)
    case StringMetric(string, service, name, state, metadata) => metric(host, service, name, string, state, metadata)
  }

  override def initialize():Unit = {
    this.internalEventsBuilder.subscribe(classOf[Metric[_]])
  }
}

trait Adapter {
  def metric(host:String, service:String, name:String, metric:Long, state:Option[String], metadata:Map[String, String])
  def metric(host:String, service:String, name:String, metric:BigDecimal, state:Option[String], metadata:Map[String, String])
  def metric(host:String, service:String, name:String, metric:Boolean, state:Option[String], metadata:Map[String, String])
  def metric(host:String, service:String, name:String, metric:String, state:Option[String], metadata:Map[String, String])
}