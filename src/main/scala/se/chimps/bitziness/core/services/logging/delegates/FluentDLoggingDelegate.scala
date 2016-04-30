package se.chimps.bitziness.core.services.logging.delegates

import akka.actor.ActorRef
import akka.http.scaladsl.model._
import se.chimps.bitziness.core.endpoints.http.{ConnectionBuilder, HttpClientEndpoint}
import se.chimps.bitziness.core.generic.Serializers.JSONSerializer
import se.chimps.bitziness.core.services.logging.LoggingDelegate

class FluentDLoggingDelegate(val settings:FluentDSettings, val fluentdEndpoint:ActorRef) extends LoggingDelegate {

  override def info(sender: String, message: String, meta: Map[String, String]): Unit = {
    fluentdEndpoint ! asJson("INFO", sender, message, meta, None)
  }

  override def warn(sender: String, message: String, meta: Map[String, String], error: Option[Throwable]): Unit = {
    fluentdEndpoint ! asJson("WARN", sender, message, meta, error)
  }

  override def error(sender: String, message: String, meta: Map[String, String], error: Option[Throwable]): Unit = {
    fluentdEndpoint ! asJson("ERROR", sender, message, meta, error)
  }

  override def debug(sender: String, message: String, meta: Map[String, String]): Unit = {
    fluentdEndpoint ! asJson("DEBUG", sender, message, meta, None)
  }

  override def unknown(level: String, sender: String, message: String, meta: Map[String, String], error: Option[Throwable]): Unit = {
    fluentdEndpoint ! asJson(level, sender, message, meta, error)
  }

  def asJson(level:String, sender:String, message:String, meta:Map[String, String], error:Option[Throwable]):(String, FluentLogMessage) = {
    (sender, FluentLogMessage(level, message, meta, stacktrace(error)))
  }

  def stacktrace(error:Option[Throwable]):Option[List[FluentStacktrace]] = {
    val trace = error.map(_.getStackTrace)

    trace.map(tr => tr.toList.map(it =>
      FluentStacktrace(it.getLineNumber, it.getFileName, it.getClassName, it.getMethodName)
    ))
  }
}

case class FluentDSettings(host:String, port:Int, secure:Boolean)
case class FluentLogMessage(level:String, message:String, meta:Map[String, String], error:Option[List[FluentStacktrace]])
case class FluentStacktrace(line:Int, file:String, className:String, method:String)

class FluentDEndpoint(val settings:FluentDSettings) extends HttpClientEndpoint with JSONSerializer {

  override def setupConnection(builder: ConnectionBuilder): ActorRef = {
    builder.host(settings.host, settings.port).build(settings.secure)
  }

  override def receive: Receive = {
    case Tuple2(logger:String, message:FluentLogMessage) => {
      send(HttpRequest(HttpMethods.POST, uri = Uri(s"/$logger"), entity = HttpEntity(ContentTypes.`application/json`, toJSON(message))))
    }
  }
}
