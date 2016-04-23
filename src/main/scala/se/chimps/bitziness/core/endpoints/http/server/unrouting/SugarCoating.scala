package se.chimps.bitziness.core.endpoints.http.server.unrouting

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.HttpEntity._
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{HttpEntity, HttpRequest}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import se.chimps.bitziness.core.generic.Codecs.Decoder

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
 *
 */
// TODO these asX methods will prolly fail on a second request.
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

  def asFormData():Future[Map[String, String]] = {
    val decoder = new StringDecoder()

    for {
      content <- dechunk[String](raw.entity, decoder)
      query <- Future(Query(content, raw.entity.contentType.charset().nioCharset))
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
}

class StringDecoder extends Decoder[ByteString, String] {
  override def decode(in:ByteString):String = in.utf8String
}