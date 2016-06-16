package se.chimps.bitziness.core.endpoints.http.server.unrouting

import java.net.InetSocketAddress
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.HttpEntity._
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.ws.{Message, UpgradeToWebSocket}
import akka.http.scaladsl.model.{HttpResponse, HttpEntity, HttpRequest}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink}
import akka.util.ByteString
import se.chimps.bitziness.core.generic.Codecs.Decoder

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
 *
 */
// TODO these asX methods will prolly fail on a second request since streams are not drained.
// TODO move durations to implicits.
trait SugarCoating {
  def inet:InetSocketAddress
  def raw:HttpRequest
  def params:Map[String, String]
  implicit def materializer:Materializer
  implicit def ec:ExecutionContext

  def param(name:String):Option[String] = params.get(name)
  def param(names:String*):Seq[Option[String]] = names.map(k => params.get(k)).seq

  def cookie(name:String):Option[String] = raw.cookies.find(c => c.name.equals(name)).map(c => c.value)

  def header(name:String):Option[String] = raw.headers.find(h => h.name().toLowerCase.equals(name.toLowerCase)).map(h => h.value())

	def accepts:Seq[String] = raw.header[Accept] match {
		case Some(accept) => accept.mediaRanges.map(range => range.value)
		case None => Seq()
	}

  def asFormData():Future[Map[String, String]] = {
    val decoder = new StringDecoder()

    for {
      content <- dechunk[String](raw.entity, decoder)
      query <- Future(Query(content, raw.entity.contentType.charsetOption.map(c => c.nioCharset()).getOrElse(Charset.defaultCharset())))
    } yield query.toMap
  }

  def asEntity[T](decoder:Decoder[ByteString, T]):Future[T] = {
    dechunk(raw.entity, decoder)
  }

  protected def dechunk[T](entity:HttpEntity, decoder:Decoder[ByteString, T]):Future[T] = {
    if (entity.isChunked()) {
      val duration = Duration(2L, TimeUnit.SECONDS)

      for {
        strict <- entity.toStrict(duration)
        content <- Future(decoder.decode(strict.data))
      } yield content
    } else {
      for {
        bytes <- entity.dataBytes.runWith(Sink.head)
        content <- Future(decoder.decode(bytes))
      } yield content
    }
  }

  def asEntityStream[T](decoder: Decoder[ByteString, T]):Future[Seq[T]] = {
    raw.entity match {
      case c:Chunked => c.chunks.runWith(Sink.fold[Seq[T], ChunkStreamPart](Seq())((seq, data) => seq ++ Seq(decoder.decode(data.data()))))
      case s:Strict => Future(Seq(decoder.decode(s.data)))
      case d:Default => d.data.runWith(Sink.fold[Seq[T], ByteString](Seq())((seq, data) => seq ++ Seq(decoder.decode(data))))
    }
  }

  def hasEntity:Boolean = !raw.entity.isKnownEmpty()
  def isChunked:Boolean = raw.entity.isChunked()
  def isUpgrade:Boolean = wsExpectedProtocols.nonEmpty

	def wsExpectedProtocols:Seq[String] = raw.header[UpgradeToWebSocket] match {
		case Some(upgrade) => upgrade.requestedProtocols
		case None => Seq()
	}

	/**
		* Handle a request for upgrade to websockets, by providing a handler (func)
		* and a default response when there was no websocket upgrade header.
		*
		* @param func the websocket handler.
		* @param noWsResponse a default "error"-handler, when the expected upgrade header was missing.
		* @return returns a HttpResponse.
		*/
  def handleWebsocket(func:(Message)=>Message, noWsResponse:HttpResponse, subProtocol:Option[String] = None):HttpResponse = {
    raw.header[UpgradeToWebSocket] match {
      case Some(upgrade) => upgrade.handleMessages(Flow[Message].map(func), subProtocol)
      case None => noWsResponse
    }
  }
}

class StringDecoder extends Decoder[ByteString, String] {
  override def decode(in:ByteString):String = in.utf8String
}