package se.chimps.bitziness.core.endpoints.http

import java.util.concurrent.TimeUnit

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import se.chimps.bitziness.core.Endpoint
import se.chimps.bitziness.core.endpoints.http.server.unrouting.{Controller, Unrouting}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration

/**
 *
 */
trait HttpServerEndpoint extends Endpoint {
  implicit val ec:ExecutionContext
  val server = createServer(new HttpServerBuilderImpl("localhost", 8080, classOf[Routed], context.system, ec))

  def createServer(builder:HttpServerBuilder):Future[ActorRef]

  // TODO this does not really belong here, neither does the Routed as default above.
  def registerController(controller:Controller):Unit = {
    server.foreach(ref => ref ! controller)
  }
}

abstract class AbstractHttpServerBinder(host:String, port:Int) extends Actor {
  implicit val system = context.system
  implicit val materializer = ActorMaterializer()

  val serverSource = Http().bind(host, port)
  def requestHandler:(HttpRequest) => Future[HttpResponse]
}

class Routed(val host:String, val port:Int)(implicit override val ec:ExecutionContext) extends AbstractHttpServerBinder(host, port) with Unrouting with ActorLogging {
  log.info(s"Http listening on $host:$port")

  override def receive:Receive = {
    case c:Controller => registerController(c)
  }

  @throws[Exception](classOf[Exception])
  override def preStart():Unit = {
    super.preStart()
    serverSource.to(Sink.foreach(conn => {
      conn.handleWithAsyncHandler(requestHandler)
    })).run()
  }
}

trait HttpServerBuilder {
  def listen(host:String, port:Int):HttpServerBuilder
  def binder(binder:Class[_<:AbstractHttpServerBinder]):HttpServerBuilder
  def build():Future[ActorRef]
}

private case class HttpServerBuilderImpl(host:String, port:Int, binder:Class[_<:AbstractHttpServerBinder], system:ActorSystem, ec:ExecutionContext) extends HttpServerBuilder {
  override def listen(host:String, port:Int):HttpServerBuilder = copy(host, port)
  override def binder(binder:Class[_<:AbstractHttpServerBinder]):HttpServerBuilder = copy(binder = binder)

  override def build():Future[ActorRef] = {
    implicit val executor = this.ec
    system.actorSelection(s"/user/$host:$port").resolveOne(Duration(3, TimeUnit.SECONDS)).recover({
      case e:Throwable => system.actorOf(Props(binder, host, port), s"$host:$port")
    })
  }
}