package se.chimps.bitziness.core.endpoints.http.server.unrouting

import akka.http.scaladsl.model.{StatusCodes, HttpResponse, HttpRequest}
import akka.stream.Materializer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 *
 */
trait Engine {

  def actions:Map[String, List[ActionDefinition]]

  def handleRequest(request:HttpRequest)(implicit materializer: Materializer):Future[HttpResponse] = {

    val response = for {
      candidate <- findCandidate(method(request), url(request))
      actionResp <- executeAction(url(request), request, candidate)
    } yield actionResp

    // TODO drain entities and what not...
    // TODO setting for printing stacktraces
    // TODO setting for request logging.

    response.recover {
      case _:NotFoundException => HttpResponse(404, entity = "Not found")
      case e:Throwable => {
        e.printStackTrace()
        HttpResponse(500, entity = "Internal server error")
      }
    }
  }

  private def findCandidate(method:String, url:String):Future[ActionDefinition] = {
    Future {
      val action = actions(method).par.find(d => url.matches(d.pathRegex.regex))

      action match {
        case Some(definition) => definition
        case None => throw new NotFoundException(method, url)
      }
    }
  }

  private def executeAction(url:String, request:HttpRequest, action:ActionDefinition)(implicit materializer: Materializer):Future[HttpResponse] = {
    val matcher = action.pathRegex.pattern.matcher(url)
    val pathParams:Map[String, String] = if (matcher.matches()) {
      val groups = (0 to matcher.groupCount()).map(b => matcher.group(b))
      action.paramNames.zip(groups.tail).toMap[String, String]
    } else {
      Map()
    }

    action.action(UnroutingRequest(request, request.uri.query.toMap ++ pathParams, materializer))
  }

  private def method(request: HttpRequest):String = {
    request.method.name
  }

  private def url(request: HttpRequest):String = {
    request.uri.path.toString()
  }
}

class NotFoundException(method:String, url:String) extends RuntimeException(s"404 Not found. ($method $url)")
