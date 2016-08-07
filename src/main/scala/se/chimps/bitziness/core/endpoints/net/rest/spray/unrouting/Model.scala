package se.chimps.bitziness.core.endpoints.net.rest.spray.unrouting

import java.net.URLDecoder

import se.chimps.bitziness.core.generic.View
import spray.http.HttpHeaders.RawHeader
import spray.http._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Model {

  trait Request {
    def method:String
    def path:String
    def header(key:String):Option[String]
    def params(key:String):Option[String]
    def params(keys:String*):Seq[Option[String]]
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

        val regex = "([a-zA-Z0-9]+)=([a-zA-Z0-9]+)".r // TODO potentially misses cases with funny data.
        val decoded = URLDecoder.decode(request.entity.asString(HttpCharsets.`UTF-8`), HttpCharsets.`UTF-8`.value)
        val split = decoded.split("&").filter(split => split.matches(regex.regex))

        split.foreach(kv => {
          val matches = regex.findAllIn(kv)
          matches.next()
          post = post ++ Map(matches.group(1) -> matches.group(2))
        })
      }

      val query = request.uri.query.toMap

      parms ++ post ++ query
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

    override def params(keys: String*): Seq[Option[String]] = keys.map(parms.get(_))

    override def cookie(key:String):Option[String] = request.cookies.find(c => c.name.equals(key)).map(c => c.content)
  }

  case class Response(code:Int, entity:Option[Entity], headers:Map[String, String], cookie:List[Cookie], chunks:List[Chunk]) {
    val data = entity match {
      case Some(BodyEntity(data, contentType)) => Some(HttpEntity(ContentType(MediaType.custom(contentType)), data))
      case Some(FileEntity(fileName, contentType)) => Some(HttpEntity(ContentType(MediaType.custom(contentType)), HttpData.fromFile(fileName)))
      case Some(ViewEntity(view)) => Some(HttpEntity(ContentType(MediaType.custom(view.contentType)), view.render()))
      case None => None
    }

    var heads = headers.map(kv => (kv._1.toLowerCase() -> kv._2)).map {
      case ("content-type", x: String) => HttpHeaders.`Content-Type`(ContentType(MediaType.custom(x)))
      case ("content-length", x: String) => HttpHeaders.`Content-Length`(x.toLong)
      case ("transfer-encoding", x: String) => HttpHeaders.`Transfer-Encoding`(x)
      case ("date", x: String) => HttpHeaders.Date(x.toDate)
      case ("server", x: String) => HttpHeaders.Server(x)
      case ("connection", x: String) => HttpHeaders.Connection(x)
      case (k: String, v: String) => RawHeader(k, v)
    }.toList

    // TODO encrypt cookies?
    private[rest] def toResponse(): HttpResponse = HttpResponse(code, data, heads ++ cookie
      .map(f => HttpHeaders.`Set-Cookie`(HttpCookie(name = f.key, content = f.value, httpOnly = true, path = f.path, expires = f.expire.map(DateTime(_)))))
      .toSeq)
  }

  trait ResponseBuilder {
    def sendEntity[T](entity:T, contentType:String = "text/html")(implicit conv:(T)=>Array[Byte]):ResponseBuilder
    def sendFile(file:String, contentType:String):ResponseBuilder
    def sendView(view:View):ResponseBuilder
    def header(key:String, value:String):ResponseBuilder
    def cookie(key:String, value:String, path:Option[String] = Some("/"), expire:Option[Long] = None):ResponseBuilder
    /**
     * This will force Encoding to chunked!
     */
    def addChunk[T](chunk:Future[T], contentType:String = "")(implicit conv:(T)=>Array[Byte]):ResponseBuilder
    def build():Response
  }

  class ResponseBuilderImpl(val code:Int = 200, val msg:Option[String] = None, val error:Option[Throwable] = None) extends ResponseBuilder {

    private var headers:Map[String, String] = Map()
    private var cookie:List[Cookie] = List()
    private var chunks:List[Chunk] = List()

    private var entity:Option[Entity] = error match {
      // TODO make this a setting, cause we dont want exceptions in production.
      case Some(e) => Some(new BodyEntity(s"<h1>An error occurred at ${msg.getOrElse(e.getMessage)}</h1><h3>${e.getMessage}</h3><p>${e.getStackTrace.mkString("</p><p>")}</p>".getBytes("utf-8"), "text/html"))
      case None => msg match {
        case Some(text) => Some(new BodyEntity(text.getBytes("utf-8"), "text/html"))
        case None => None
      }
    }

    override def sendEntity[T](data:T, contentType:String = "text/html")(implicit conv:(T)=>Array[Byte]):ResponseBuilder = {
      entity = Some(new BodyEntity(conv(data), contentType))
      this
    }

    override def header(key:String, value:String):ResponseBuilder = {
      headers = headers ++ Map(key -> value)
      this
    }

    override def sendFile(file:String, contentType:String):ResponseBuilder = {
      entity = Some(new FileEntity(file, contentType))
      this
    }

    override def sendView(view: View): ResponseBuilder = {
      entity = Some(ViewEntity(view))
      this
    }

    override def cookie(key: String, value: String, path:Option[String] = Some("/"), expire:Option[Long] = None): ResponseBuilder = {
      cookie = cookie ++ List(Cookie(key, value, path, expire))
      this
    }

    override def addChunk[T](chunk: Future[T], contentType:String = "")(implicit conv: (T) => Array[Byte]): ResponseBuilder = {
      header("Transfer-Encoding", "chunked")
      chunks = chunks ++ List(new Chunk(chunk.map(conv(_)), contentType))
      this
    }

    override def build():Response = new Response(code, entity, headers, cookie, chunks)
  }

  sealed trait Entity {}
  case class BodyEntity(data:Array[Byte], contentType:String) extends Entity
  case class FileEntity(fileName:String, contentType:String) extends Entity
  case class ViewEntity(view:View) extends Entity

  case class Chunk(body:Future[Array[Byte]], contentType:String)

  case class Cookie(key:String, value:String, path:Option[String], expire:Option[Long])

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
        new ResponseBuilderImpl(301, Some(uri)).header("Location", uri)
      }
    }

    object TODO {
      def apply():ResponseBuilder = {
        new ResponseBuilderImpl(501, Some("TODO"))
      }
    }

    object BadRequest {
      def apply():ResponseBuilder = {
        new ResponseBuilderImpl(400, Some("Bad request"))
      }
    }
  }

  implicit def str2Date(in:String):Str2Date = {
    new Str2Date(in)
  }

  class Str2Date(val str:String) {
    def toDate:DateTime = {
      DateTime.fromIsoDateTimeString(str).getOrElse(DateTime.now) // FUGLY, needs to be remade rather asap...
    }
  }
}
