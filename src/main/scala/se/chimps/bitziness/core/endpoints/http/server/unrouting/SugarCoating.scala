package se.chimps.bitziness.core.endpoints.http.server.unrouting

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.HttpEntity.{Chunked, Default, Strict}
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{ContentType, HttpRequest}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

/**
 *
 */
// TODO these asX methods will prolly fail on a second request.
// TODO move durations to implicits.
// TODO add a asEntityStream with fold instead of foreach.
trait SugarCoating {
  def raw:HttpRequest
  def params:Map[String, String]
  implicit def materializer:Materializer

  def param(name:String):Option[String] = params.get(name)
  def param(names:String*):Seq[Option[String]] = names.map(k => params.get(k)).seq

  def cookie(name:String):Option[String] = raw.cookies.find(c => c.name.equals(name)).map(c => c.value)

  def header(name:String):Option[String] = raw.headers.find(h => h.name().toLowerCase.equals(name.toLowerCase)).map(h => h.value())

  def asFormData():Future[Map[String, String]] = {
    val decoder = new StringDecoder()

    if (raw.entity.isChunked()) {
      val duration = Duration(2L, TimeUnit.SECONDS)

      for {
        strict <- raw.entity.toStrict(duration)
        content <- decoder.decode(strict.contentType)(strict.data)
        query <- Future(Query(content, strict.contentType.charset().nioCharset))
      } yield query.toMap
    } else {
      for {
        bytes <- raw.entity.dataBytes.runWith(Sink.head)
        content <- decoder.decode(raw.entity.contentType())(bytes)
        query <- Future(Query(content, raw.entity.contentType().charset().nioCharset))
      } yield query.toMap
    }
  }

  def asEntity[T](decoder:Decoder[T]):Future[T] = {
    if (raw.entity.isChunked()) {
      val duration = Duration(2L, TimeUnit.SECONDS)

      for {
        strict <- raw.entity.toStrict(duration)
        content <- decoder.decode(strict.contentType)(strict.data)
      } yield content
    } else {
      for {
        bytes <- raw.entity.dataBytes.runWith(Sink.head)
        content <- decoder.decode(raw.entity.contentType())(bytes)
      } yield content
    }
  }

  def asEntityStream[T](decoder: Decoder[T])(func:(Future[T])=>Unit):Unit = {
    raw.entity match {
      case c:Chunked => c.chunks.map(chunk => decoder.decode(c.contentType)(chunk.data())).runForeach(func)
      case s:Strict => func(decoder.decode(s.contentType)(s.data))
      case d:Default => d.dataBytes.map(decoder.decode(d.contentType)(_)).runForeach(func)
    }
  }

  def hasEntity:Boolean = !raw.entity.isKnownEmpty()
  def isChunked:Boolean = raw.entity.isChunked()
}

trait Decoder[T] {
  def decode(contentType: ContentType)(data:ByteString):Future[T]
}

class StringDecoder extends Decoder[String] {
  override def decode(contentType:ContentType)(data:ByteString):Future[String] = Future(data.decodeString(contentType.charset().value)).mapTo[String]
}