package se.chimps.bitziness.core.endpoints.net.rest.spray.unrouting

import java.util.concurrent.TimeUnit

import akka.testkit.TestProbe
import se.chimps.bitziness.core.endpoints.net.rest.{EndpointDefinition}
import se.chimps.bitziness.core.endpoints.net.rest.spray.unrouting.Framework.Controller
import spray.http._

import scala.concurrent.duration.Duration

/**
 * A smallish trait to help test controllers.
 * TODO make this available in "core" for other projects to use.
 */
trait ControllerTesting extends Engine {
  def controller:Controller
  def serviceProbe:TestProbe
  def connectionProbe:TestProbe

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

  def assertResponse(status:Int, body:List[String], request:HttpRequest, clue:String = "") = {
//    println(response.entity.asString(HttpCharsets.`UTF-8`))
    handle(request, connectionProbe.ref)

    if (body != null) {
      val parts:List[String] = if (body.size > 1) { List("") ++ body ++ List("") } else { body }

      parts.foreach { b =>
        connectionProbe.expectMsgPF(Duration(3L, TimeUnit.SECONDS), "No message can do...") {
          case res: HttpResponse => {
            assert(res.status.intValue == status, clue)
            if (body == null) {
              assert(res.entity.isEmpty)
            } else {
//            println(res.entity.asString(HttpCharsets.`UTF-8`))
              assert(res.entity.asString(HttpCharsets.`UTF-8`).startsWith(b), clue)
            }
          }
          case start: ChunkedResponseStart => {
            assert(start.message.status.intValue == status, "Chunked status was not correct.")
          }
          case chunk: MessageChunk => {
//            println(chunk.data.asString(HttpCharsets.`UTF-8`))
            // TODO there can be out of order delivery here...
            assert(!chunk.data.isEmpty, "MessageChunk had no body!")
            assert(parts.contains(chunk.data.asString(HttpCharsets.`UTF-8`)), s"Chunk ${chunk.data.asString(HttpCharsets.`UTF-8`)} was not part of, ${parts}.")
          }
          case end: ChunkedMessageEnd => {
          }
        }
      }
    } else {
      connectionProbe.expectMsgPF(Duration(3L, TimeUnit.SECONDS), "No message can do... bodyless version") {
        case res: HttpResponse => {
          assert(res.status.intValue == status, clue)
          if (body == null) {
            assert(res.entity.isEmpty)
          } else {
//          println(res.entity.asString(HttpCharsets.`UTF-8`))
            assert(res.entity.asString(HttpCharsets.`UTF-8`).startsWith(body.head), clue)
          }
        }
      }
    }
  }

  controller(serviceProbe.ref)
  addActions(EndpointDefinition(null, Map("" -> controller)))
}