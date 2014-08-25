package se.chimps.bitziness.core.endpoints.rest.spray.unrouting

import java.net.URLDecoder

import spray.http.HttpHeaders.RawHeader
import spray.http._

object Model {

  trait Request {
    def method:String
    def path:String
    def header(key:String):Option[String]
    def params(key:String):Option[String]
    def session(key:String):Option[String]
    def cookie(key:String):Option[String]
    def entity[T](implicit conv:(Array[Byte])=>Option[T]):Option[T]
  }

  case class RequestImpl(request:HttpRequest, meta:ActionMetadata) extends Request {

    def createParms():Map[String, String] = {
      val uri = request.uri.path.toString()
      val matches = meta.regex.pattern.matcher(uri)

      val parms:Map[String, String] = if (matches.matches()) {
        val i = 0 to matches.groupCount()
        val k = i.map(b => matches.group(b))
        meta.keyNames.zip(k.tail).toMap[String, String]
      } else {
        Map()
      }

      var post = Map[String, String]()

      if (request.method.isEntityAccepted && request.headers.
        exists(h => h.value.equals("application/x-www-form-urlencoded"))) {

        val regex = "([a-zA-Z0-9]+)=([a-zA-Z0-9]+)".r
        val decoded = URLDecoder.decode(request.entity.asString(HttpCharsets.`UTF-8`), HttpCharsets.`UTF-8`.value)
        val split = decoded.split("&").filter(split => split.matches(regex.regex))

        split.foreach(kv => {
          val matches = regex.findAllIn(kv)
          matches.next()
          post = post ++ Map(matches.group(1) -> matches.group(2))
        })
      }
      parms ++ post
    }

    private val parms:Map[String, String] = createParms()

    override def method:String = request.method.name

    override def path:String = request.uri.path.toString()

    override def entity[T](implicit conv:(Array[Byte]) => Option[T]):Option[T] = conv(request.entity.data.toByteArray)

    override def header(key:String):Option[String] = request.headers.find(h => h.name.equals(key)) match {
      case Some(header:HttpHeader) => Some(header.value)
      case _ => None
    }

    override def params(key:String):Option[String] = parms.get(key)

    // TODO create a session store.
    override def session(key:String):Option[String] = None

    override def cookie(key:String):Option[String] = None
  }

  case class Response(code:Int, entity:Option[Array[Byte]], headers:Map[String, String]) {
    val data = entity match {
      case Some(bytes) => Some(HttpEntity(bytes))
      case None => None
    }

    val heads = headers.map(kv => RawHeader(kv._1, kv._2)).toList

    private[unrouting] def toResponse():HttpResponse = HttpResponse(code, data, heads)
  }

  trait ResponseBuilder {
    def withEntity[T](entity:T)(implicit conv:(T)=>Option[Array[Byte]]):ResponseBuilder
    def header(key:String, value:String):ResponseBuilder
    def build():Response
  }

  class ResponseBuilderImpl(val code:Int = 200, val msg:Option[String] = None, val error:Option[Throwable] = None) extends ResponseBuilder {
    private var entity:Option[Array[Byte]] = error match {
      // TODO make this a setting, cause we dont want exceptions in production.
      case Some(e) => Some(s"<h1>${msg.getOrElse(e.getMessage)}</h1><p>${e.getStackTrace.mkString("</p><p>")}</p>".getBytes("utf-8"))
      case None => None
    }
    private var headers:Map[String, String] = Map()

    override def withEntity[T](data:T)(implicit conv:(T)=>Option[Array[Byte]]):ResponseBuilder = {
      entity = conv(data)
      this
    }

    override def header(key:String, value:String):ResponseBuilder = {
      headers = headers ++ Map(key -> value)
      this
    }

    override def build():Response = new Response(code, entity, headers)
  }

  object Responses {

    object Ok {
      def apply():ResponseBuilder = {
        new ResponseBuilderImpl()
      }
    }

    object NotFound {
      def apply():ResponseBuilder = {
        new ResponseBuilderImpl(404)
      }

      def apply(msg:String):ResponseBuilder = {
        new ResponseBuilderImpl(404, Some(msg))
      }
    }

    object Created {
      def apply():ResponseBuilder = {
        new ResponseBuilderImpl(201)
      }
    }

    object Error {
      def apply():ResponseBuilder = {
        new ResponseBuilderImpl(500)
      }

      def apply(error:Throwable):ResponseBuilder = {
        new ResponseBuilderImpl(500, None, Some(error))
      }

      def apply(msg:String, error:Throwable):ResponseBuilder = {
        new ResponseBuilderImpl(500, Some(msg), Some(error))
      }
    }

    object NoContent {
      def apply():ResponseBuilder = {
        new ResponseBuilderImpl(204)
      }
    }

    object Redirect {
      def apply(uri:String):ResponseBuilder = {
        new ResponseBuilderImpl(301, Some(uri))
      }
    }
  }
}
