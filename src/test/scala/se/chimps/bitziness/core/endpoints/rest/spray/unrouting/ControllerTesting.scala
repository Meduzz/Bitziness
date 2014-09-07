package se.chimps.bitziness.core.endpoints.rest.spray.unrouting

import se.chimps.bitziness.core.endpoints.rest.EndpointDefinition
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Framework.Controller
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Model.Responses.{Error, NotFound}
import spray.http._

/**
 * A smallish trait to hel controllertesting.
 */
trait ControllerTesting extends Engine {
  def controller:Controller
  lazy val definitions = new EndpointDefinition(null, null, Map("" -> controller))

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

  def assertResponse(status:Int, body:String, response:HttpResponse, clue:String = "") = {
//    println(response.entity.asString(HttpCharsets.`UTF-8`))
    response.entity.isEmpty // lazy issue?
    assert(response.status.intValue == status, clue)
    if (body == null) {
      assert(response.entity.isEmpty)
    } else {
      assert(response.entity.asString(HttpCharsets.`UTF-8`).startsWith(body), clue)
    }
  }
}