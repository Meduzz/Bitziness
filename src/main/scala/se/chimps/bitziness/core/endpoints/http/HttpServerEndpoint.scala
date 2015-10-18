package se.chimps.bitziness.core.endpoints.http

import akka.actor.{ActorSystem, ActorLogging}
import akka.event.LoggingAdapter
import akka.http.ServerSettings.Timeouts
import akka.http.ServerSettings
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.{IncomingConnection, ServerBinding}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.io.Inet.SocketOption
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.Config
import se.chimps.bitziness.core.Endpoint

import scala.concurrent.Future
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
 *
 */
trait HttpServerEndpoint extends Endpoint with ActorLogging {
  implicit val system = context.system
  implicit val mat = ActorMaterializer()

  private [http] lazy val serverSource = Http().bind(host, port, log = log)
  private [http] var server:Future[ServerBinding] = _

  @throws[Exception](classOf[Exception])
  override def preStart():Unit = {
    super.preStart()
    log.info(s"Akka http listening @ $host:$port.")
    server = createServer(new HttpServerBuilderImpl(serverSource, settings = ServerSettings(context.system), handler = requestHandler, logger = log))
  }

  def createServer(builder:HttpServerBuilder):Future[ServerBinding]
  def requestHandler:(HttpRequest) => Future[HttpResponse]

  def host:String
  def port:Int
}

trait HttpServerBuilder {
//  def akkaParallelism(cores:Int):HttpServerBuilder
//  def settings(fn:(ServerSettingsBuilder)=>ServerSettings):HttpServerBuilder
//  def settingsFromConfig(config:Config):HttpServerBuilder
  def build()(implicit system:ActorSystem, mat:ActorMaterializer):Future[ServerBinding]
}

trait ServerSettingsBuilder {
  def backlog(number:Int):ServerSettingsBuilder
  def maxConnections(number:Int):ServerSettingsBuilder
  def verboseErrorMessages(torf:Boolean):ServerSettingsBuilder
  def socketOption(option:SocketOption):ServerSettingsBuilder
  def idleTimeout(timeout:FiniteDuration):ServerSettingsBuilder
  def bindTimeout(timeout:FiniteDuration):ServerSettingsBuilder
  def build():ServerSettings
}

private case class HttpServerBuilderImpl(source:Source[IncomingConnection, Future[ServerBinding]],
                                         cores:Int = 4,
                                         settings:ServerSettings,
                                         handler:(HttpRequest)=>Future[HttpResponse],
                                         logger:LoggingAdapter) extends HttpServerBuilder {

//  override def settingsFromConfig(config:Config):HttpServerBuilder = copy(settings = ServerSettings.create(config))

//  override def akkaParallelism(cores:Int):HttpServerBuilder = copy(cores = cores)

/*
  override def settings(fn:(ServerSettingsBuilder) => ServerSettings):HttpServerBuilder = {
    copy(settings = fn(new ServerSettingsBuilderImpl(settings)))
  }
*/

  override def build()(implicit system:ActorSystem, mat:ActorMaterializer):Future[ServerBinding] = {
    source.to(Sink.foreach(conn => {
      conn.handleWithAsyncHandler(handler)
    })).run()
  }
}

private case class ServerSettingsBuilderImpl(backlog:Int,
                                             idleTimeout:Duration,
                                             verboseErrors:Boolean,
                                             bindTimeout:FiniteDuration,
                                             socketOpts:Seq[SocketOption],
                                             maxConnections:Int,
                                             settings:ServerSettings) extends ServerSettingsBuilder {

  def this(settings:ServerSettings) = {
    this(settings.backlog,
      settings.timeouts.idleTimeout.unary_-,
      settings.verboseErrorMessages,
      settings.timeouts.bindTimeout,
      settings.socketOptions.toSeq,
      settings.maxConnections,
      settings)
  }

  override def backlog(number:Int):ServerSettingsBuilder = copy(backlog = number)

  override def idleTimeout(timeout:FiniteDuration):ServerSettingsBuilder = copy(idleTimeout = timeout)

  override def verboseErrorMessages(torf:Boolean):ServerSettingsBuilder = copy(verboseErrors = torf)

  override def bindTimeout(timeout:FiniteDuration):ServerSettingsBuilder = copy(bindTimeout = timeout)

  override def socketOption(option:SocketOption):ServerSettingsBuilder = copy(socketOpts = socketOpts ++ Seq(option))

  override def maxConnections(number:Int):ServerSettingsBuilder = copy(maxConnections = number)

  override def build():ServerSettings = settings.copy(maxConnections = maxConnections, backlog = backlog, verboseErrorMessages = verboseErrors, timeouts = Timeouts(idleTimeout,bindTimeout))
}
