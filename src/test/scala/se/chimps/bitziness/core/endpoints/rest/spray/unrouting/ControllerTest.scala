package se.chimps.bitziness.core.endpoints.rest.spray.unrouting

import akka.actor.{ActorNotFound, ActorSystem, ActorRef}
import akka.testkit.{TestProbe, TestKitBase}
import org.scalatest.FunSuite
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Framework.{Controller}
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Model.Request
import spray.http._

class ControllerTest extends FunSuite with TestKitBase with ControllerTesting {
  lazy val controller = new MyController

  implicit lazy val system = ActorSystem()

  test("the controller should respond nicely") {
    val probe = TestProbe()
    controller(probe.ref)

    assertResponse(200, "Hello!", request(get("/")), "No gentle Hello!...")
    assertResponse(404, "Nobody home at /spam.", request(get("/spam")), "Expected a 404...")
    assertResponse(200, "asdf", request(post("/gringo", "asdf")))
    assertResponse(200, "They are all here!", request(get("/a/b/c/d")), "No one home at /a/b/c/d")
    assertResponse(200, "q/q", request(get("/q/q")), "First match should be returned when multiple actions match.")
    assertResponse(200, "q/:q", request(get("/q/:q")), "'Regexes' are valid paths.")
    assertResponse(200, "spam", request(get("/test/spam")), "PathParams did not work as expected.")
    assertResponse(200, "Hello a and b!", request(get("/hello/a/and/b")), "A more complicated path failed.")
    assertResponse(200, "Your id #300.", request(post("/form/400", "id=300").withHeaders(HttpHeaders.`Content-Type`(ContentType(MediaType.custom("application/x-www-form-urlencoded"))))))
    assertResponse(200, "Your id #400.", request(post("/form/400", "{text:1}").withHeaders(HttpHeaders.`Content-Type`(ContentType(MediaType.custom("application/json"))))))
    assertResponse(200, null, request(get("/headers")), "Headers blew up.")
    assertResponse(200, "* {color:#AAA;}", request(get("/file")))
    assertResponse(200, "file.ending", request(get("/static/file.ending")))
    assertResponse(500, "<h1>An error occurred at", request(get("/crash")), "The crash, should crash successfully.")
  }

}

class MyController extends Controller {
  import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Model.Responses._

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
      val a = req.params("a").getOrElse("fail")
      val b = req.params("b").getOrElse("fail")
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
  }

  implicit def str2Bytes(str:String):Array[Byte] = str.getBytes("utf-8")
  implicit def bytes2Str(bytes:Array[Byte]):Option[String] = Some(new String(bytes, "utf-8"))
}