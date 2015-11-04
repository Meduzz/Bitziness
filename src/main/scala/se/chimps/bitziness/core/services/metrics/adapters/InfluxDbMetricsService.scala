package se.chimps.bitziness.core.services.metrics.adapters

import akka.actor.ActorRef
import akka.http.scaladsl.model.{HttpResponse, Uri, HttpMethods, HttpRequest}
import se.chimps.bitziness.core.endpoints.http.{ConnectionBuilder, HttpClientEndpoint}
import se.chimps.bitziness.core.services.metrics.MetricService

class InfluxDbMetricsService(val settings:InfluxSettings, override val host:String) extends MetricService {

  var influxdb:ActorRef = _

  override def metric(host:String, service: String, name: String, metric: Long, state: Option[String], metadata: Map[String, String]): Unit = {
    influxdb ! s"$name,host=$host,service=$service${convert(state)}${meta(metadata)} value=${metric}i"
  }

  override def metric(host:String, service: String, name: String, metric: BigDecimal, state: Option[String], metadata: Map[String, String]): Unit = {
    influxdb ! s"$name,host=$host,service=$service${convert(state)}${meta(metadata)} value=${metric.toString()}"
  }

  override def metric(host:String, service: String, name: String, metric: Boolean, state: Option[String], metadata: Map[String, String]): Unit = {
    val value = metric match {
      case true => "t"
      case _ => "f"
    }
    influxdb ! s"$name,host=$host,service=$service${convert(state)}${meta(metadata)} value=${value}"
  }

  override def metric(host:String, service: String, name: String, metric: String, state: Option[String], metadata: Map[String, String]): Unit = {
    influxdb ! s"$name,host=$host,service=$service${convert(state)}${meta(metadata)} value=${metric}"
  }

  override def initialize(): Unit = {
    super.initialize()
    influxdb = initEndpoint(new InfluxDbEndpoint(self, settings), s"influxdb-${settings.host}:${settings.port}")
  }

  def meta(metadata:Map[String, String]):String = {
    val meta = metadata.map(m => s"${m._1}=${m._2}").mkString(",")

    if (meta.length > 0) {
      s",$meta"
    } else {
      ""
    }
  }

  def convert(state:Option[String]):String = {
    state.map(s => s",state=$s").getOrElse("")
  }
}

case class InfluxSettings(host:String, port:Int, db:String, username:Option[String], password:Option[String], secure:Boolean = false, buffer:Int = 100)

class InfluxDbEndpoint(val service:ActorRef, val settings:InfluxSettings) extends HttpClientEndpoint {
  var buffer:Seq[String] = Seq()

  override def setupConnection(builder: ConnectionBuilder): ActorRef = {
    builder.host(settings.host, settings.port).build(settings.secure)
  }

  def uri:String = s"/write?${settings.db}${settings.username.map(u => s"&u=$u")}${settings.password.map(p => s"&p=$p")}"

  override def receive: Receive = {
    case s:String => bufferAndSend(s)
    case res:HttpResponse =>
  }

  def bufferAndSend(metric:String) = {
    if (buffer.size + 1 >= 100) {
      val data = (buffer ++ Seq(metric)).mkString("\n")
      connection ! HttpRequest(HttpMethods.POST, Uri(uri), entity = data)
    } else {
      buffer = buffer ++ Seq(metric)
    }
  }
}