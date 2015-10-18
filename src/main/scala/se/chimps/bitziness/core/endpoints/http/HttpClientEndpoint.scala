package se.chimps.bitziness.core.endpoints.http

import akka.actor.{ActorSystem, ActorRef, Props}
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.ConnectionPoolSettings
import akka.http.scaladsl.{HttpsContext, Http}
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.{Sink, Source, Flow}
import se.chimps.bitziness.core.Endpoint

import scala.annotation.tailrec
import scala.concurrent.{Future, Promise}

/**
 *
 */
trait HttpClientEndpoint extends Endpoint {
  implicit val system = context.system
  implicit val materializer = ActorMaterializer()
  val connection = setupConnection(new ConnectionBuilderImpl)

  def setupConnection(builder:ConnectionBuilder):ActorRef

  def send(request:HttpRequest):Future[HttpResponse] = {
    val promise = Promise[HttpResponse]()
    connection ! RequestTransport(request, promise)
    promise.future
  }
}

trait ConnectionBuilder {
  def host(address:String, port:Int):ConnectionBuilder
  def connectionPoolSettings(settings:ConnectionPoolSettings):ConnectionBuilder
  def logger(log:LoggingAdapter):ConnectionBuilder
  def securityContext(context:HttpsContext):ConnectionBuilder
  def build(useHttps:Boolean):ActorRef
}

private class ConnectionBuilderImpl(implicit materializer:Materializer, system:ActorSystem) extends ConnectionBuilder {
  var host:String = "localhost"
  var port:Int = 8080
  var settings:ConnectionPoolSettings = _
  var log:LoggingAdapter = _
  var context:Option[HttpsContext] = None

  override def host(address:String, port:Int):ConnectionBuilder = {
    host = address
    this.port = port
    this
  }

  override def connectionPoolSettings(settings:ConnectionPoolSettings):ConnectionBuilder = {
    this.settings = settings
    this
  }

  override def logger(log:LoggingAdapter):ConnectionBuilder = {
    this.log = log
    this
  }

  override def securityContext(context:HttpsContext):ConnectionBuilder = {
    this.context = Some(context)
    this
  }

  // TODO this got ... not so pretty.
  override def build(secure:Boolean):ActorRef = {
    if (settings != null && log != null) {
      if (secure) {
        Http().cachedHostConnectionPoolTls[Promise[HttpResponse]](host, port, settings, context, log).to(Sink.foreach(tuple => {
          val (response, promise) = tuple
          promise.complete(response)
        })).runWith(Source.actorPublisher(Props(classOf[HttpClientActor])))
      } else {
        Http().cachedHostConnectionPool[Promise[HttpResponse]](host, port, settings, log).to(Sink.foreach(tuple => {
          val (response, promise) = tuple
          promise.complete(response)
        })).runWith(Source.actorPublisher(Props(classOf[HttpClientActor])))
      }
    } else if (settings != null) {
      if (secure) {
        Http().cachedHostConnectionPoolTls[Promise[HttpResponse]](host, port, settings, context).to(Sink.foreach(tuple => {
          val (response, promise) = tuple
          promise.complete(response)
        })).runWith(Source.actorPublisher(Props(classOf[HttpClientActor])))
      } else {
        Http().cachedHostConnectionPool[Promise[HttpResponse]](host, port, settings).to(Sink.foreach(tuple => {
          val (response, promise) = tuple
          promise.complete(response)
        })).runWith(Source.actorPublisher(Props(classOf[HttpClientActor])))
      }
    } else if (log != null) {
      if (secure) {
        Http().cachedHostConnectionPoolTls[Promise[HttpResponse]](host, port = port, log = log, httpsContext = context).to(Sink.foreach(tuple => {
          val (response, promise) = tuple
          promise.complete(response)
        })).runWith(Source.actorPublisher(Props(classOf[HttpClientActor])))
      } else {
        Http().cachedHostConnectionPool[Promise[HttpResponse]](host, port = port, log = log).to(Sink.foreach(tuple => {
          val (response, promise) = tuple
          promise.complete(response)
        })).runWith(Source.actorPublisher(Props(classOf[HttpClientActor])))
      }
    } else {
      if (secure) {
        Http().cachedHostConnectionPoolTls[Promise[HttpResponse]](host, port, httpsContext = context).to(Sink.foreach(tuple => {
          val (response, promise) = tuple
          promise.complete(response)
        })).runWith(Source.actorPublisher(Props(classOf[HttpClientActor])))
      } else {
        Http().cachedHostConnectionPool[Promise[HttpResponse]](host, port).to(Sink.foreach(tuple => {
          val (response, promise) = tuple
          promise.complete(response)
        })).runWith(Source.actorPublisher(Props(classOf[HttpClientActor])))
      }
    }
  }
}

case class RequestTransport(request:HttpRequest, promise:Promise[HttpResponse])

private class HttpClientActor extends ActorPublisher[(HttpRequest, Promise[HttpResponse])] {

  val maxBufferSize = 100
  var buffer = Seq[RequestTransport]()

  override def receive:Receive = {
    case r:RequestTransport => {
      println(s"has new request. buffer:${buffer.size}")
      if (buffer.size == maxBufferSize) {
        r.promise.failure(new RuntimeException("Buffer is full."))
      } else {
        buffer = buffer ++ Seq(r)
        deliver()
      }
    }
    case Request(_) => deliver()
    case Cancel => context.stop(self) // TODO do we need to clean buffer?
  }

  @tailrec
  private final def deliver():Unit = {
    val demand = if (totalDemand <= Int.MaxValue) { totalDemand.toInt } else { Int.MaxValue }

    println(s"If demand ($demand) is higher than 0 we'll deliver a request now.")

    if (demand > 0 && buffer.nonEmpty) {
      val (data, keep) = buffer.splitAt(demand)
      buffer = keep
      send(data)
      deliver()
    }
  }

  private final def send(pieces:Seq[RequestTransport]) = {
    println(s"Sending ${pieces.size} requests.")
    pieces.foreach { piece =>
      onNext((piece.request, piece.promise))
    }
  }
}