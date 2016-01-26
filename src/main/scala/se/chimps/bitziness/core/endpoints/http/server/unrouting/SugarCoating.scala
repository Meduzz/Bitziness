package se.chimps.bitziness.core.endpoints.http.server.unrouting

import java.util.concurrent.TimeUnit

import akka.http.scaladsl.model.HttpEntity._
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{ContentType, HttpRequest}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration

/**
 *
 */
// TODO these asX methods will prolly fail on a second request.
// TODO move durations to implicits.
trait SugarCoating {
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

  def asEntityStream[T](decoder: Decoder[T]):Future[Seq[T]] = {
    raw.entity match {
      case c:Chunked => c.chunks.runWith(Sink.fold[Seq[Future[T]], ChunkStreamPart](Seq())((seq, data) => seq ++ Seq(decoder.decode(c.contentType)(data.data())))).map(Future.sequence(_)).flatMap(s => s)
      case s:Strict => decoder.decode(s.contentType)(s.data).map(Seq(_))
      case d:Default => d.data.runWith(Sink.fold[Seq[Future[T]], ByteString](Seq())((seq, data) => seq ++ Seq(decoder.decode(d.contentType)(data)))).map(Future.sequence(_)).flatMap(s => s)
    }
  }

  def hasEntity:Boolean = !raw.entity.isKnownEmpty()
  def isChunked:Boolean = raw.entity.isChunked()
}

trait Decoder[T] {
  def decode(contentType: ContentType)(data:ByteString)(implicit ec:ExecutionContext):Future[T]
}

class StringDecoder extends Decoder[String] {
  override def decode(contentType:ContentType)(data:ByteString)(implicit ec:ExecutionContext):Future[String] = Future(data.decodeString(contentType.charset().value)).mapTo[String]
}