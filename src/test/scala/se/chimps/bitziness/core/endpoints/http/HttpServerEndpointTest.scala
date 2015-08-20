package se.chimps.bitziness.core.endpoints.http

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpEntity, StatusCodes, HttpResponse}
import akka.stream.ActorMaterializer
import org.scalatest.FunSuite
import se.chimps.bitziness.core.endpoints.http.client.RequestFactory
import se.chimps.bitziness.core.endpoints.http.server.unrouting.{UnroutingRequest, Action, Controller, Unrouting}

import scala.util.Success
import scala.concurrent.ExecutionContext.Implicits.global

/**
 *
 */
class HttpServerEndpointTest extends FunSuite with Unrouting with RequestFactory {

  implicit val materializer = ActorMaterializer()(ActorSystem())

  registerController(new MyController)

  test("how the api feels") {
    val response = handleRequest(request("GET", "/vafan"))

    response.onComplete {
      case Success(resp) => resp.entity.dataBytes.map(_.utf8String).runForeach(str => assert(str.equals("vafan"), s"Response was $str, instead of vafan."))
    }
  }
}

class MyController extends Controller {
  get("/:fetta", Action.sync((req:UnroutingRequest) => {
    HttpResponse(StatusCodes.OK, entity = HttpEntity(req.param("fetta").getOrElse("nope")))
  }))
}