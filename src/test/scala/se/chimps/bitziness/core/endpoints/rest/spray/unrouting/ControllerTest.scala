package se.chimps.bitziness.core.endpoints.rest.spray.unrouting

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.testkit.{TestKitBase, TestProbe}
import org.scalatest.FunSuite
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Framework.Controller
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Model.Request
import spray.http._

import scala.concurrent.Future

class ControllerTest extends FunSuite with TestKitBase with ControllerTesting {
  implicit lazy val system = ActorSystem("RestController")

  lazy val log = Logging(system, getClass.getName)

  lazy val controller = new MyController
  lazy val serviceProbe = TestProbe()
  lazy val connectionProbe = TestProbe()

  test("the controller should respond nicely") {
    assertResponse(200, List("Hello!"), get("/"), "No gentle Hello!...")
    assertResponse(404, List("Nobody home at /spam."), get("/spam"), "Expected a 404...")
    assertResponse(200, List("asdf"), post("/gringo", "asdf"))
    assertResponse(200, List("They are all here!"), get("/a/b/c/d"), "No one home at /a/b/c/d")
    assertResponse(200, List("q/q"), get("/q/q"), "First match should be returned when multiple actions match.")
    assertResponse(200, List("q/:q"), get("/q/:q"), "'Regexes' are valid paths.")
    assertResponse(200, List("spam"), get("/test/spam"), "PathParams did not work as expected.")
    assertResponse(200, List("Hello a and b!"), get("/hello/a/and/b"), "A more complicated path failed.")
    assertResponse(200, List("Your id #300."), post("/form/400", "id=300").withHeaders(HttpHeaders.`Content-Type`(ContentType(MediaType.custom("application/x-www-form-urlencoded")))))
    assertResponse(200, List("Your id #400."), post("/form/400", "{text:1}").withHeaders(HttpHeaders.`Content-Type`(ContentType(MediaType.custom("application/json")))))
    assertResponse(200, null, get("/headers"), "Headers blew up.")
    assertResponse(200, List("* {color:#AAA;}"), get("/file"))
    assertResponse(200, List("file.ending"), get("/static/file.ending"))
    assertResponse(500, List("<h1>An error occurred at"), get("/crash"), "The crash, should crash successfully.")
    assertResponse(200, List("this", "rocks"), get("/chunked"), "Little bitty chunks, were nowhere to be found!")
    assertResponse(200, List("has.key"), get("/specull/has.key"), "Specull was not that specull!")
  }

}

class MyController extends Controller {
  import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Model.Responses._
  import scala.concurrent.ExecutionContext.Implicits.global

  def apply(service:ActorRef)= {
    get("/", Action {(req:Request) => Ok().sendEntity("Hello!").build() })
    post("/gringo", Action {(req:Request) => Ok().sendEntity(req.entity[String].getOrElse("fail!")).build() })
    get("/a/b/c/d", Action {(req:Request) => Ok().sendEntity("They are all here!").build() })
    get("/q/q", Action {(req:Request) => Ok().sendEntity("q/q").build() })
    get("/q/:q", Action {(req:Request) => Ok().sendEntity("q/:q").build() })
    get("/test/:test", Action { req =>
      val test = req.params("test").getOrElse("fail!")
      Ok().sendEntity(test).build()
    })
    get("/hello/:a/and/:b", Action { req =>
      val Seq(Some(a), Some(b)) = req.params("a", "b")
      Ok().sendEntity(s"Hello ${a} and ${b}!").build()
    })
    post("/form/:id", Action { req =>
      val id = req.params("id").getOrElse("fail")
      Ok().sendEntity(s"Your id #${id}.").build()
    })
    get("/crash", Action(() => throw new RuntimeException("TBD")))
    get("/headers", Action(() => Ok().header("Date", "2014-01-01T00:00:00").header("Server", "spam").header("Connection", "ok").build()))
    get("/file", Action(() => Ok().sendFile(getClass.getResource("/static.css").getPath, "text/stylesheet").build()))
    get("/static/:file.:ending", Action(req => Ok().sendEntity(s"${req.params("file").get}.${req.params("ending").get}").build()))
    get("/chunked", Action(req => Ok().addChunk(Future("this")).addChunk(Future("rocks")).build()))
    get("/specull/:key", Action(req => Ok().sendEntity(req.params("key").get).build()), Map("key" -> "([a-z\\.]+)"))
  }
}