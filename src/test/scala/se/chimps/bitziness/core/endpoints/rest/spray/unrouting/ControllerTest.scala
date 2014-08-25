package se.chimps.bitziness.core.endpoints.rest.spray.unrouting

import akka.actor.{ActorSystem, ActorRef}
import akka.testkit.{TestProbe, TestKitBase}
import org.scalatest.FunSuite
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Framework.{ModuleReader, Controller}
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Model.Request
import spray.http._

class ControllerTest extends FunSuite with TestKitBase {

  implicit lazy val system = ActorSystem()

  test("the controller should respond nicely") {
    val probe = TestProbe()
    val controller = new MyController() with ModuleReader
    controller(probe.ref)

    assertResponse(200, "Hello!", controller.request(get("/")), "No gentle Hello!...")
    assertResponse(404, "Nobody home at /spam.", controller.request(get("/spam")), "Expected a 404...")
    assertResponse(200, "asdf", controller.request(post("/gringo", "asdf")))
    assertResponse(200, "They are all here!", controller.request(get("/a/b/c/d")), "No one home at /a/b/c/d")
    assertResponse(200, "q/q", controller.request(get("/q/q")), "First match should be returned when multiple actions match.")
    assertResponse(404, "Nobody home at /q/:q.", controller.request(get("/q/:q")), "Regexes are not valid paths.")
    assertResponse(200, "spam", controller.request(get("/test/spam")), "PathParams did not work as expected.")
    assertResponse(200, "Hello a and b!", controller.request(get("/hello/a/and/b")), "A more complicated path failed.")
    assertResponse(200, "Your id #300.", controller.request(post("/form/400", "id=300").withHeaders(HttpHeaders.`Content-Type`(ContentType(MediaType.custom("application/x-www-form-urlencoded"))))))
    assertResponse(200, "Your id #400.", controller.request(post("/form/400", "{text:1}").withHeaders(HttpHeaders.`Content-Type`(ContentType(MediaType.custom("application/json"))))))
  }

  def request(method:HttpMethod, uri:String, body:Option[String]):HttpRequest = {
    HttpRequest(method, Uri(uri), List(), body match {
      case Some(b) => HttpEntity(b.getBytes("utf-8"))
      case None => null
    })
  }

  def get(uri:String):HttpRequest = {
    request(HttpMethods.GET, uri, None)
  }

  def post(uri:String, body:String):HttpRequest = {
    request(HttpMethods.POST, uri, Some(body))
  }

  def assertResponse(status:Int, body:String, response:HttpResponse, clue:String = "") = {
//    println(response.entity.asString(HttpCharsets.`UTF-8`))
    assert(response.status.intValue == status, clue)
    if (body == null) {
      assert(response.entity.isEmpty)
    } else {
      assert(response.entity.asString(HttpCharsets.`UTF-8`).equals(body), clue)
    }
  }
}

class MyController extends Controller {
  import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Model.Responses._

  def apply(service:ActorRef)= {
    get(Action("/") {(req:Request) => Ok().withEntity("Hello!").build() })
    post(Action("/gringo") {(req:Request) => Ok().withEntity(req.entity[String].getOrElse("fail!")).build() })
    get(Action("/a/b/c/d") {(req:Request) => Ok().withEntity("They are all here!").build() })
    get(Action("/q/q") {(req:Request) => Ok().withEntity("q/q").build() })
    get(Action("/q/:q") {(req:Request) => Ok().withEntity("q/:q").build() })
    get(Action("/test/:test") { req =>
      val test = req.params("test").getOrElse("fail!")
      Ok().withEntity(test).build()
    })
    get(Action("/hello/:a/and/:b") { req =>
      val a = req.params("a").getOrElse("fail")
      val b = req.params("b").getOrElse("fail")
      Ok().withEntity(s"Hello ${a} and ${b}!").build()
    })
    post(Action("/form/:id") { req =>
      val id = req.params("id").getOrElse("fail")
      Ok().withEntity(s"Your id #${id}.").build()
    })
  }

  implicit def str2Bytes(str:String):Option[Array[Byte]] = Some(str.getBytes("utf-8"))
  implicit def bytes2Str(bytes:Array[Byte]):Option[String] = Some(new String(bytes, "utf-8"))
}