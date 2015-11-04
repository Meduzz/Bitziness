package se.chimps.bitziness.core.endpoints.http

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpEntity.{ChunkStreamPart, Chunked, Strict}
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import org.scalatest.FunSuite
import se.chimps.bitziness.core.endpoints.http.client.RequestBuilders
import se.chimps.bitziness.core.endpoints.http.server.unrouting._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
 *
 */
class HttpServerEndpointTest extends FunSuite with Unrouting with RequestBuilders {

  implicit val materializer = ActorMaterializer()(ActorSystem("akka-http-server"))

  registerController(new MyController)

  private val chunks = List("we", "like", "chunks").map(ChunkStreamPart(_))

  test("controllers, actions and responses") {
    expectResponse(200, Seq("spam"))(handleRequest(request("GET", "/param/spam")))
    expectResponse(200, Seq("Hello a and b!"))(handleRequest(request("GET", "/extract/a/and/b")))
    expectResponse(404, Seq())(handleRequest(request("GET", "/PARAM/CAPSLOCK")))
    expectResponse(500, Seq())(handleRequest(request("GET", "/crash")))
    expectResponse(200, Seq("file.html"))(handleRequest(request("GET", "/static/file.html")))
    expectResponse(200, Seq("some.kind.of.key"))(handleRequest(request("GET", "/key/some.kind.of.key")))
    expectResponse(200, Seq("Your name is John Doe."))(handleRequest(request("POST", "/form").withEntity("surname=Doe&name=John")))
    expectResponse(200, Seq("ew", "ekil", "sknuhc"))(handleRequest(request("PUT", "/chunks").withEntity(Chunked(ContentTypes.`text/plain`, Source(chunks))))) // chunks needs to be of scala.collection.immutable apparently.
  }

  def expectResponse(status:Int, body:Seq[String], headers:Seq[HttpHeader] = Seq())(futureResponse:Future[HttpResponse]): Unit = {
    val response = Await.result[HttpResponse](futureResponse, Duration(3L, TimeUnit.SECONDS))

    assert(response.status.intValue() == status, s"Status code was not $status, but ${response.status.intValue()}.")
    headers.foreach(h => {
      assert(response.getHeader(h.name()).isDefined, s"Header ${h.name()} was not defined.")
      assert(response.getHeader(h.name()).get.value().equals(h.value()), s"Header value of ${h.name()} did not match, was ${response.getHeader(h.name()).get.value()}, expected ${h.value()}.")
    })

    val entityData = response.entity match {
      case s:Strict => Seq(s.data.utf8String)
      case d:Chunked => {
        var seq = Seq[String]()
        val unchunking = d.dataBytes.map(_.utf8String).runForeach{ bit =>
          seq :+= bit
        }
        Await.ready(unchunking, Duration(3L, TimeUnit.SECONDS))
        seq
      }
      case _ => Seq()
    }

    body.foreach(data => {
      assert(entityData.contains(data), s"Body did not contain $data.")
    })
  }
}

class MyController extends Controller with ResponseBuilders {
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
      Ok().withEntity(s"Your name is ${data("name")} ${data("surname")}.")
    }
  })
  put("/chunks", Action.sync { req =>
    var chunks:List[String] = List()
    req.asEntityStream(new StringDecoder())(str => str.foreach(chunks :+= _))
    Ok().withEntity(Chunked(ContentTypes.`text/plain`, Source(chunks.map(_.reverse).map(ChunkStreamPart(_)))))
  })
}