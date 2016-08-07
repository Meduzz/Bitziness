package se.chimps.bitziness.core.endpoints.net.http.server.unrouting

import java.net.InetSocketAddress

import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.stream.Materializer

import scala.concurrent.{ExecutionContext, Future}

/**
 *
 */
trait Engine {

  private[unrouting] def actions:Map[String, List[ActionDefinition]]

  def handleRequest(inet:InetSocketAddress, request:HttpRequest)(implicit materializer: Materializer, ec:ExecutionContext):Future[HttpResponse] = {

    val response = for {
      candidate <- findCandidate(method(request), url(request))
      actionResp <- executeAction(url(request), inet, request, candidate)
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

  private def findCandidate(method:String, url:String)(implicit ec:ExecutionContext):Future[ActionDefinition] = {
    Future {
      val action = actions(method).par.find(d => url.matches(d.pathRegex.regex))

      action match {
        case Some(definition) => definition
        case None => throw new NotFoundException(method, url)
      }
    }
  }

  private def executeAction(url:String, inet:InetSocketAddress, request:HttpRequest, action:ActionDefinition)(implicit materializer: Materializer, ec:ExecutionContext):Future[HttpResponse] = {
    val matcher = action.pathRegex.pattern.matcher(url)
    val pathParams:Map[String, String] = if (matcher.matches()) {
      val groups = (0 to matcher.groupCount()).map(b => matcher.group(b))
      action.paramNames.zip(groups.tail).toMap[String, String]
    } else {
      Map()
    }

    action.action(UnroutingRequest(inet, request, request.uri.query().toMap ++ pathParams, materializer, ec))
  }

  private def method(request: HttpRequest):String = {
    request.method.name
  }

  private def url(request: HttpRequest):String = {
    request.uri.path.toString()
  }
}

class NotFoundException(method:String, url:String) extends RuntimeException(s"404 Not found. ($method $url)")
