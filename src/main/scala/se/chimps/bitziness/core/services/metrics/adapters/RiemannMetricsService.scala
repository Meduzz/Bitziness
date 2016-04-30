package se.chimps.bitziness.core.services.metrics.adapters

import com.aphyr.riemann.client.{RiemannClient, RiemannBatchClient, IRiemannClient}
import se.chimps.bitziness.core.services.metrics.MetricsDelegate

class RiemannMetricsService(val host:String, val settings:RiemannSettings) extends MetricsDelegate {

  val client:IRiemannClient = new RiemannBatchClient(RiemannClient.tcp(settings.host, settings.port), settings.buffer)
	client.connect()

  override def metric(service: String, name: String, metric: Long, state: Option[String], metadata: Map[String, String]): Unit = {
    val event = client.event()
      .host(host)
      .service(s"$service.$name")
      .metric(metric)

    state.foreach(event.state)
    metadata.foreach(map => event.attribute(map._1, map._2))

    client.sendEvent(event.build())
  }

  override def metric(service: String, name: String, metric: BigDecimal, state: Option[String], metadata: Map[String, String]): Unit = {
    val event = client.event()
      .host(host)
      .service(s"$service.$name")
      .metric(metric.floatValue())

    state.foreach(event.state)
    metadata.foreach(map => event.attribute(map._1, map._2))

    client.sendEvent(event.build())
  }

  override def metric(service: String, name: String, metric: Boolean, state: Option[String], metadata: Map[String, String]): Unit = {
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

  override def metric(service: String, name: String, metric: String, state: Option[String], metadata: Map[String, String]): Unit = {
    val event = client.event()
      .host(host)
      .service(s"$service.$name")
      .tag(metric)

    state.foreach(event.state)
    metadata.foreach(map => event.attribute(map._1, map._2))

    client.sendEvent(event.build())
  }
}

case class RiemannSettings(host:String, port:Int, buffer:Int)