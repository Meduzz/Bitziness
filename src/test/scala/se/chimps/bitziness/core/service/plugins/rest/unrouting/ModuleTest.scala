package se.chimps.bitziness.core.service.plugins.rest.unrouting

import akka.actor.{ActorSystem, ActorRef}
import akka.testkit.{TestProbe, TestKitBase, TestActorRef}
import org.scalatest.FunSuite
import se.chimps.bitziness.core.service.plugins.rest.unrouting.Spray.{ModuleReader, Module, Action}
import spray.http._

class ModuleTest extends FunSuite with TestKitBase {

  implicit lazy val system = ActorSystem()

  test("the controller should respond nicely") {
    val probe = TestProbe()
    val controller = new Controller() with ModuleReader
    controller(probe.ref)

    assertResponse(200, "Hello!", controller.request(get("/")), "No gentle Hello!...")
    assertResponse(404, null, controller.request(get("/spam")), "Expected a 404...")
    assertResponse(200, "asdf", controller.request(post("/gringo", "asdf")))
    assertResponse(200, "They are all here!", controller.request(get("/a/b/c/d")), "No one home at /a/b/c/d")
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

class Controller extends Module {
  def apply(service:ActorRef)= {
    get("/", Action((req)=>HttpResponse(200, HttpEntity("Hello!".getBytes("utf-8")))))
    post("/gringo", Action((req)=>HttpResponse(200, req.entity.asString(HttpCharsets.`UTF-8`))))
    get("/a/b/c/d", Action((req)=>HttpResponse(200, HttpEntity("They are all here!".getBytes("utf-8")))))
  }
}