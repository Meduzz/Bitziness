package se.chimps.bitziness.core.endpoints.rest.spray.unrouting

import akka.testkit.TestProbe
import se.chimps.bitziness.core.endpoints.rest.Routes
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Framework.Controller
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Model.{FileEntity, BodyEntity, Response, RequestImpl}
import spray.http._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Try, Failure, Success}

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

  // TODO rewrite this to verify the response in an actorProbe.
  def request(req:HttpRequest):Future[HttpResponse] = {
    val uri = req.uri.path.toString()
    hasMatch(req) match {
      case Some(actionMetadata: ActionMetadata) =>
        Try(actionMetadata.action(new RequestImpl(req, actionMetadata))) match {
          case Success(f: Future[Response]) => f.map(_.toResponse())
          case Failure(e: Throwable) => Future(fiveZeroZero(uri, e).toResponse())
        }
      case None => Future(fourZeroFour(uri).toResponse())
    }
  }
}