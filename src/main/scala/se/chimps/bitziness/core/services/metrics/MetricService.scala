package se.chimps.bitziness.core.services.metrics

import se.chimps.bitziness.core.Service
import se.chimps.bitziness.core.generic.Events
import se.chimps.bitziness.core.generic.metrics._

class MetricService(val delegate:MetricsDelegate) extends Service with Events {

  override def handle:Receive = {
    case LongMetric(long, service, name, state, metadata) => delegate.metric(service, name, long, state, metadata)
    case DecimalMetric(deci, service, name, state, metadata) => delegate.metric(service, name, deci, state, metadata)
    case BooleanMetric(bool, service, name, state, metadata) => delegate.metric(service, name, bool, state, metadata)
    case StringMetric(string, service, name, state, metadata) => delegate.metric(service, name, string, state, metadata)
  }

  override def initialize():Unit = {
    internalEventsBuilder.subscribe(classOf[Metric[_]])
  }
}

trait MetricsDelegate {
  def metric(service:String, name:String, metric:Long, state:Option[String], metadata:Map[String, String])
  def metric(service:String, name:String, metric:BigDecimal, state:Option[String], metadata:Map[String, String])
  def metric(service:String, name:String, metric:Boolean, state:Option[String], metadata:Map[String, String])
  def metric(service:String, name:String, metric:String, state:Option[String], metadata:Map[String, String])
}