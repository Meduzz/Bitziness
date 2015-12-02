package se.chimps.bitziness.core.services.metrics.adapters

import com.aphyr.riemann.client.{RiemannClient, RiemannBatchClient, IRiemannClient}
import se.chimps.bitziness.core.services.metrics.MetricService

class RiemannMetricsService(val settings:RiemannSettings, override val host:String) extends MetricService {

  var client:IRiemannClient = _

  override def metric(host: String, service: String, name: String, metric: Long, state: Option[String], metadata: Map[String, String]): Unit = {
    val event = client.event()
      .host(host)
      .service(s"$service.$name")
      .metric(metric)

    state.foreach(event.state)
    metadata.foreach(map => event.attribute(map._1, map._2))

    client.sendEvent(event.build())
  }

  override def metric(host: String, service: String, name: String, metric: BigDecimal, state: Option[String], metadata: Map[String, String]): Unit = {
    val event = client.event()
      .host(host)
      .service(s"$service.$name")
      .metric(metric.floatValue())

    state.foreach(event.state)
    metadata.foreach(map => event.attribute(map._1, map._2))

    client.sendEvent(event.build())
  }

  override def metric(host: String, service: String, name: String, metric: Boolean, state: Option[String], metadata: Map[String, String]): Unit = {
    val value = metric match {
      case true => 1
      case _ => 0
    }

    val event = client.event()
      .host(host)
      .service(s"$service.$name")
      .metric(value)

    state.foreach(event.state)
    metadata.foreach(map => event.attribute(map._1, map._2))

    client.sendEvent(event.build())
  }

  override def metric(host: String, service: String, name: String, metric: String, state: Option[String], metadata: Map[String, String]): Unit = {
    val event = client.event()
      .host(host)
      .service(s"$service.$name")
      .tag(metric)

    state.foreach(event.state)
    metadata.foreach(map => event.attribute(map._1, map._2))

    client.sendEvent(event.build())
  }

  override def initialize(): Unit = {
    super.initialize()
    client = new RiemannBatchClient(RiemannClient.tcp(settings.host, settings.port), settings.buffer)
    client.connect()
  }

  @throws[Exception](classOf[Exception])
  override def postStop():Unit = {
    super.postStop()
    client.flush()
    client.close()
  }
}

case class RiemannSettings(host:String, port:Int, buffer:Int)