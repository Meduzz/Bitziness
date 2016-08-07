package se.chimps.bitziness.core.services.metrics.adapters

import akka.actor.ActorRef
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, Uri}
import se.chimps.bitziness.core.endpoints.net.http.{ConnectionBuilder, HttpClientEndpoint}
import se.chimps.bitziness.core.services.metrics.MetricsDelegate

class InfluxDbMetricsDelegate(val influxdbEndpoint:ActorRef, val host:String) extends MetricsDelegate {

  override def metric(service: String, name: String, metric: Long, state: Option[String], metadata: Map[String, String]): Unit = {
    influxdbEndpoint ! s"${escape(name)},host=${escape(host)},service=${escape(service)}${convert(state)}${meta(metadata)} value=${metric}i"
  }

  override def metric(service: String, name: String, metric: BigDecimal, state: Option[String], metadata: Map[String, String]): Unit = {
    influxdbEndpoint ! s"${escape(name)},host=${escape(host)},service=${escape(service)}${convert(state)}${meta(metadata)} value=${metric.toString()}"
  }

  override def metric(service: String, name: String, metric: Boolean, state: Option[String], metadata: Map[String, String]): Unit = {
    val value = metric match {
      case true => "t"
      case _ => "f"
    }

    influxdbEndpoint ! s"${escape(name)},host=${escape(host)},service=${escape(service)}${convert(state)}${meta(metadata)} value=$value"
  }

  override def metric(service: String, name: String, metric: String, state: Option[String], metadata: Map[String, String]): Unit = {
    influxdbEndpoint ! s"""${escape(name)},host=${escape(host)},service=${escape(service)}${convert(state)}${meta(metadata)} value=\"$metric\""""
  }

  def meta(metadata:Map[String, String]):String = {
    val meta = metadata.map(m => s"${m._1}=${escape(m._2)}").mkString(",")

    if (meta.length > 0) {
      s",$meta"
    } else {
      ""
    }
  }

  def convert(state:Option[String]):String = {
    state.map(s => s",state=${escape(s)}").getOrElse("")
  }

  def escape(str:String):String = str.replace(" ", "\\ ")
}

case class InfluxSettings(host:String, port:Int, db:String, username:Option[String], password:Option[String], secure:Boolean = false, buffer:Int = 100)

class InfluxDbEndpoint(val service:ActorRef, val settings:InfluxSettings) extends HttpClientEndpoint {
  var buffer:Seq[String] = Seq()

  // TODO implement a send every X second feature.

  override def setupConnection(builder: ConnectionBuilder): ActorRef = {
    builder.host(settings.host, settings.port).build(settings.secure)
  }

  def uri:String = s"/write?db=${settings.db}${settings.username.map(u => s"&u=$u").getOrElse("")}${settings.password.map(p => s"&p=$p").getOrElse("")}"

  override def receive: Receive = {
    case s:String => bufferAndSend(s)
    case res:HttpResponse =>
  }

  def bufferAndSend(metric:String) = {
    if (buffer.size + 1 >= settings.buffer) {
      val data = (buffer ++ Seq(metric)).mkString("\n")
      send(HttpRequest(HttpMethods.POST, uri = Uri(uri), entity = data))
      buffer = Seq()
    } else {
      buffer = buffer ++ Seq(metric)
    }
  }
}
