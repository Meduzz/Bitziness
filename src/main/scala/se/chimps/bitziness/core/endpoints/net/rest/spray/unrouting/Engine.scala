package se.chimps.bitziness.core.endpoints.net.rest.spray.unrouting

import akka.actor.{ActorRef}
import akka.event.{LoggingAdapter}
import akka.pattern.PipeToSupport
import se.chimps.bitziness.core.endpoints.net.rest.EndpointDefinition
import se.chimps.bitziness.core.endpoints.net.rest.spray.unrouting.Model.{Chunk, Request, Response, RequestImpl}
import se.chimps.bitziness.core.endpoints.net.rest.spray.unrouting.Model.Responses.{Error, NotFound}
import spray.http._

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure, Try}
import scala.util.matching.Regex

/**
 * A base trait with all the code necessary to route requests.
 */
trait Engine extends PipeToSupport {
  def log:LoggingAdapter

  private var actions:Map[String, Map[Regex, ActionMetadata]] = Map()

  protected def request(action:Action, req:Request):Future[Response] = {
    Try(action(req)) match {
        case Success(f:Future[Response]) => f
        case Failure(e:Throwable) => Future(fiveZeroZero(req.path, e))
    }
  }

  protected def handle(req:HttpRequest, sender:ActorRef) = {
    val uri = req.uri.path.toString()
    hasMatch(req) match {
      case Some(actionMetadata:ActionMetadata) => {
        request(actionMetadata.action, new RequestImpl(req, actionMetadata)).onComplete {
          case Success(response:Response) => {
            if (response.chunks.size == 0) {
              sender ! response.toResponse()
            } else {
              sender ! ChunkedResponseStart(response.toResponse())
              handleResponseChunks(response.chunks, sender)
            }
          }
          case Failure(e) => sender ! fiveZeroZero(uri, e).toResponse()
        }
      }
      case None => sender ! fourZeroFour(uri).toResponse()
    }
  }

  @tailrec
  private def handleResponseChunks(chunks:List[Chunk], sender:ActorRef): Unit = {

    if (chunks.size > 0) {
      chunks.head.body.map(MessageChunk(_)).pipeTo(sender)
    }

    if (chunks.tail.size > 0) {
      handleResponseChunks(chunks.tail, sender)
    } else {
      sender ! ChunkedMessageEnd()
    }
  }

  protected def hasMatch(req:HttpRequest):Option[ActionMetadata] = {
    val uri = req.uri.path.toString()

    req.method match {
      case HttpMethods.GET => findMatch(actions("GET"), uri)
      case HttpMethods.POST => findMatch(actions("POST"), uri)
      case HttpMethods.PUT => findMatch(actions("PUT"), uri)
      case HttpMethods.DELETE => findMatch(actions("DELETE"), uri)
    }
  }

  private def findMatch(someActions:Map[Regex, ActionMetadata], path:String):Option[ActionMetadata] = {
    val allMatches = someActions.filter(action => path.matches(action._1.regex) || action._2.path.equals(path))

    val exactPath = allMatches.find(pair => pair._2.path.equals(path))

    // there are still room for action-bingo here if there are more than one regex match...
    exactPath.orElse(allMatches.filter(pair => !pair._2.equals(path)).headOption) match {
      case Some((_:Regex, m:ActionMetadata)) => Some(m)
      case None => None
    }
  }

  protected def fourZeroFour(missingPath:String):Response = {
    NotFound(s"Nobody home at ${missingPath}.").build()
  }

  protected def fiveZeroZero(errorPath:String, error:Throwable):Response = {
    Error(errorPath, error).build()
  }

  private implicit def strToBytes(msg:String):Array[Byte] = {
    msg.getBytes("utf-8")
  }

  def addActions(routes:EndpointDefinition):Unit = {
    routes.routes.foreach(ctrl => {
      val root = ctrl._1
      val controller = ctrl._2

      controller.actionDefinitions.map { definition =>
        (definition.method -> createMetadata(s"$root${definition.path}", definition.action, definition.paramex))
      }.foreach { meta =>
        val verbActions = actions.getOrElse(meta._1, Map[Regex, ActionMetadata]()) ++ Map(meta._2.regex -> meta._2)
        actions = actions ++ Map(meta._1 -> verbActions)
      }
    })
  }

  private def createMetadata(uri:String, action:Action, paramex:Map[String, String]):ActionMetadata = {
    val findPathParamsRegex = ":([a-zA-Z0-9]+)".r
    val pathParamsRegex = "(:[a-zA-Z0-9]+)".r
    val pathNames = findPathParamsRegex.findAllIn(uri).map(m => m.substring(1)).toList
    val pathRegex = pathNames.foldLeft(uri)((url, name) => s"(:$name)".r.replaceAllIn(url, paramex.getOrElse(name, "([a-zA-Z0-9]+)"))).r

    new ActionMetadata(uri, action, pathRegex, pathNames)
  }
}

case class ActionMetadata(path:String, action:Action, regex:Regex, keyNames:List[String])
case class ActionDefinition(method:String, path:String, action:Action, paramex:Map[String, String] = Map())
