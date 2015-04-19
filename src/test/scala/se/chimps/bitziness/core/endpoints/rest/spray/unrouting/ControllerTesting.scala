package se.chimps.bitziness.core.endpoints.rest.spray.unrouting

import akka.testkit.TestProbe
import se.chimps.bitziness.core.endpoints.rest.Routes
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Framework.Controller
import spray.http._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

/**
 * A smallish trait to help test controllers.
 * TODO make this available in "core" for other projects to use.
 */
trait ControllerTesting extends Engine {
  def controller:Controller
  def probe:TestProbe

  controller(probe.ref)
  addActions(Routes(Map("" -> controller)))

  def buildRequest(method:HttpMethod, uri:String, body:Option[String]):HttpRequest = {
    HttpRequest(method, Uri(uri), List(), body match {
      case Some(b) => HttpEntity(b.getBytes("utf-8"))
      case None => null
    })
  }

  def get(uri:String):HttpRequest = {
    buildRequest(HttpMethods.GET, uri, None)
  }

  def post(uri:String, body:String):HttpRequest = {
    buildRequest(HttpMethods.POST, uri, Some(body))
  }

  def put(uri:String, body:String):HttpRequest = {
    buildRequest(HttpMethods.PUT, uri, Some(body))
  }

  def delete(uri:String):HttpRequest = {
    buildRequest(HttpMethods.DELETE, uri, None)
  }

  def assertResponse(status:Int, body:String, response:Future[HttpResponse], clue:String = "") = {
//    println(response.entity.asString(HttpCharsets.`UTF-8`))
    response.onComplete {
      case Success(res) => {
        res.entity.isEmpty // lazy issue?
        assert(res.status.intValue == status, clue)
        if (body == null) {
          assert(res.entity.isEmpty)
        } else {
          assert(res.entity.asString(HttpCharsets.`UTF-8`).startsWith(body), clue)
        }
      }
      case Failure(e) => throw e
    }
  }
}