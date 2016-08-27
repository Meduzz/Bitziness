package se.chimps.bitziness.core.endpoints.net.http

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpEntity.{ChunkStreamPart, Chunked, Strict}
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures
import se.chimps.bitziness.core.endpoints.net.http.client.RequestBuilders
import se.chimps.bitziness.core.endpoints.net.http.server.unrouting._
import se.chimps.bitziness.core.generic.codecs.StringDecoder

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
 *
 */
class HttpServerEndpointTest extends FunSuite with Unrouting with RequestBuilders with ScalaFutures {

  implicit val materializer = ActorMaterializer()(ActorSystem("akka-http-server"))
  implicit val ec = Implicits.global

  registerController(new MyController)
  registerController(new MyOtherController)

  private val chunks = List("we", "like", "chunks").map(ChunkStreamPart(_))

  test("controllers, actions and responses") {
    expectResponse(200, Seq("spam"))(handleRequest(new InetSocketAddress(12345), request("GET", "/param/spam")))
    expectResponse(200, Seq("Hello a and b!"))(handleRequest(new InetSocketAddress(12345), request("GET", "/extract/a/and/b")))
    expectResponse(404, Seq())(handleRequest(new InetSocketAddress(12345), request("GET", "/PARAM/CAPSLOCK")))
    expectResponse(500, Seq())(handleRequest(new InetSocketAddress(12345), request("GET", "/crash")))
    expectResponse(200, Seq("file.html"))(handleRequest(new InetSocketAddress(12345), request("GET", "/static/file.html")))
    expectResponse(200, Seq("some.kind.of.key"))(handleRequest(new InetSocketAddress(12345), request("GET", "/key/some.kind.of.key")))
    expectResponse(200, Seq("Your name is John Doe."))(handleRequest(new InetSocketAddress(12345), request("POST", "/form").withEntity("surname=Doe&name=John")))
    expectResponse(200, Seq("ew", "ekil", "sknuhc"))(handleRequest(new InetSocketAddress(12345), request("PUT", "/chunks").withEntity(Chunked(ContentTypes.`text/plain(UTF-8)`, Source(chunks))))) // chunks needs to be of scala.collection.immutable apparently.
    expectResponse(200, Seq("54321"))(handleRequest(new InetSocketAddress(54321), request("GET", "/ip")))
  }

  def expectResponse(status:Int, body:Seq[String], headers:Seq[HttpHeader] = Seq())(futureResponse:Future[HttpResponse]): Unit = {
    val response = Await.result[HttpResponse](futureResponse, Duration(3L, TimeUnit.SECONDS))

    assert(response.status.intValue() == status, s"Status code was not $status, but ${response.status.intValue()}.")
    headers.foreach(h => {
      assert(response.getHeader(h.name()).isPresent, s"Header ${h.name()} was not defined.")
      assert(response.getHeader(h.name()).get.value().equals(h.value()), s"Header value of ${h.name()} did not match, was ${response.getHeader(h.name()).get.value()}, expected ${h.value()}.")
    })

    val entityData:Seq[String] = response.entity match {
      case s:Strict => Seq(s.data.utf8String)
      case d:Chunked => {
        val unchunked = d.dataBytes.map(_.utf8String).runFold[Seq[String]](Seq[String]())((a, b) => { a ++ Seq(b) })

        whenReady(unchunked) { chunks =>
          chunks
        }
      }
      case _ => Seq()
    }

    body.foreach(data => {
      assert(entityData.contains(data), s"Body did not contain $data.")
    })
  }
}

class MyController extends Controller with ResponseBuilders with Validation {
  import Implicits.global

  get("/param/:param1", Action.sync((req:UnroutingRequest) => {
    Ok().withEntity(HttpEntity(req.param("param1").getOrElse("nope")))
  }))
  get("/extract/:a/and/:b", Action.sync { req =>
    val Seq(a, b) = req.param("a", "b")
    Ok().withEntity(s"Hello ${a.getOrElse("")} and ${b.getOrElse("")}!")
  })
  get("/crash", Action.sync { req =>
    throw new RuntimeException("TBD")
  })
  get("/static/:file.:ending", Action.sync { req =>
    val Seq(file, ending) = req.param("file", "ending")
    Ok().withEntity(s"${file.getOrElse("")}.${ending.getOrElse("")}")
  })
  get("/key/:rest", Action.sync { req =>
    Ok().withEntity(req.param("rest").getOrElse("failed"))
  }, Map("rest" -> "([a-z\\.]+)"))
  post("/form", Action { req =>
    req.asFormData().map { data =>
      val nameField = validate[String](data("name"), str => if (str.lengthBetween(4, 10) && str.notNull) { None } else { Some("Name is null, or not between 4-10 chars.") })
      val surnameField = validate[String](data("surname"), str => if (str.lengthBetween(0, 3) && str.notNull) { None } else { Some("Surname is null or not between 0-3 chars.") })

      val textField = for {
        name <- nameField
        surname <- surnameField
      } yield s"Your name is ${data("name")} ${data("surname")}."

      textField match {
        case Valid(text) => Ok().withEntity(text)
        case Invalid(text, msg) => Error().withEntity(msg)
      }
    }
  })
}

class MyOtherController extends Controller with ResponseBuilders {
  import Implicits.global

  put("/chunks", Action { req =>
    req.asEntityStream(new StringDecoder()).map(chunks => {
      import scala.collection.immutable.Seq
      val bits = Seq.concat(chunks.map(_.reverse).map(ChunkStreamPart(_)))
      Ok().withEntity(Chunked(ContentTypes.`text/plain(UTF-8)`, Source(bits)))
    })
  })
	get("/ip", Action.sync { req =>
		val port = req.inet.getPort

		Ok().withEntity(port.toString)
	})
}