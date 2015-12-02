package se.chimps.bitziness.core.services.logging.adapters

import akka.actor.{Stash, ActorRef}
import akka.http.scaladsl.model._
import akka.util.ByteString
import se.chimps.bitziness.core.endpoints.http.{ConnectionBuilder, HttpClientEndpoint}
import se.chimps.bitziness.core.generic.ActorFactory
import se.chimps.bitziness.core.generic.Serializers.JSONSerializer
import se.chimps.bitziness.core.services.logging.LoggingService

class FluentDLoggingService(val settings:FluentDSettings, val server:String, val service:String) extends LoggingService {

  var fluentd:ActorRef = _

  override def info(sender: String, message: String, meta: Map[String, String]): Unit = {
    fluentd ! asJson("INFO", sender, message, meta, None)
  }

  override def warn(sender: String, message: String, meta: Map[String, String], error: Option[Throwable]): Unit = {
    fluentd ! asJson("WARN", sender, message, meta, error)
  }

  override def error(sender: String, message: String, meta: Map[String, String], error: Option[Throwable]): Unit = {
    fluentd ! asJson("ERROR", sender, message, meta, error)
  }

  override def debug(sender: String, message: String, meta: Map[String, String]): Unit = {
    fluentd ! asJson("DEBUG", sender, message, meta, None)
  }

  override def unknown(level: String, sender: String, message: String, meta: Map[String, String], error: Option[Throwable]): Unit = {
    fluentd ! asJson(level, sender, message, meta, error)
  }

  override def initialize(): Unit = {
    super.initialize()
    val factory = ActorFactory(s"fluentd-${settings.host}:${settings.port}", () => new FluentDEndpoint(self, settings))
    fluentd = initEndpoint(factory)
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

class FluentDEndpoint(val service:ActorRef, val settings:FluentDSettings) extends HttpClientEndpoint with JSONSerializer {

  override def setupConnection(builder: ConnectionBuilder): ActorRef = {
    builder.host(settings.host, settings.port).build(settings.secure)
  }

  override def receive: Receive = {
    case Tuple2(logger:String, message:FluentLogMessage) => {
      send(HttpRequest(HttpMethods.POST, uri = Uri(s"/$logger"), entity = HttpEntity(ContentTypes.`application/json`, toJSON(message))))
    }
  }
}
