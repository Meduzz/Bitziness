package se.chimps.bitziness.core.endpoints.http

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import se.chimps.bitziness.core.Endpoint
import se.chimps.bitziness.core.endpoints.http.server.unrouting.{Controller, Unrouting}

import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Promise}
import scala.util.{Try, Failure, Random, Success}

/**
 *
 */
trait HttpServerEndpoint extends Endpoint {
  import context.dispatcher
  val server = createServer(new HttpServerBuilderImpl("localhost", 8080, classOf[Routed], context.system))

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
  def requestHandler(remote:InetSocketAddress):(HttpRequest) => Future[HttpResponse]
}

class Routed(val host:String, val port:Int) extends AbstractHttpServerBinder(host, port) with Unrouting with ActorLogging {
  implicit val ec = context.dispatcher
  log.info(s"Http listening on $host:$port")

  override def receive:Receive = {
    case c:Controller => registerController(c)
  }

  @throws[Exception](classOf[Exception])
  override def preStart():Unit = {
    super.preStart()
    serverSource.to(Sink.foreach(conn => {
      conn.handleWithAsyncHandler(requestHandler(conn.remoteAddress))
    })).run()
  }
}

trait HttpServerBuilder {
  def listen(host:String, port:Int):HttpServerBuilder
  def binder(binder:Class[_<:AbstractHttpServerBinder]):HttpServerBuilder
  def build():Future[ActorRef]
}

private case class HttpServerBuilderImpl(host:String, port:Int, binder:Class[_<:AbstractHttpServerBinder], system:ActorSystem) extends HttpServerBuilder {
  override def listen(host:String, port:Int):HttpServerBuilder = copy(host, port)
  override def binder(binder:Class[_<:AbstractHttpServerBinder]):HttpServerBuilder = copy(binder = binder)

  override def build():Future[ActorRef] = {
		val promise = Promise[ActorRef]()
		Try(system.actorOf(Props(binder, host, port), s"$host:$port")) match {
			case s:Success[ActorRef] => {
				promise.complete(s)
			}
			case f:Failure[ActorRef] => {
				createActor(promise)
			}
		}
		promise.future
  }

	private def createActor(promise:Promise[ActorRef], lock:Int = 0):Unit = {
		import scala.concurrent.ExecutionContext.Implicits.global

		Thread.sleep(Random.nextInt(1000))
		val actor = system.actorSelection(s"/user/$host:$port").resolveOne(Duration(3, TimeUnit.SECONDS))
		actor.onComplete {
			case s:Success[ActorRef] => {
				promise.complete(s)
			}
			case f:Failure[ActorRef] => {
				if (lock > 3) {
					promise.complete(f)
				} else {
					createActor(promise, lock + 1)
				}
			}
		}
	}
}