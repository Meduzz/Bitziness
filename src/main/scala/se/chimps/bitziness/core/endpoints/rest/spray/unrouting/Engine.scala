package se.chimps.bitziness.core.endpoints.rest.spray.unrouting

import org.slf4j.LoggerFactory
import se.chimps.bitziness.core.endpoints.rest.{Routes}
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Model.{Response, RequestImpl}
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Model.Responses.{Error, NotFound}
import spray.http.{HttpMethods, HttpResponse, HttpRequest}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure, Try}
import scala.util.matching.Regex

/**
 * A base trait with all the code necessary to route requests.
 */
trait Engine {
  private var actions:Map[String, Map[Regex, ActionMetadata]] = Map()
  private val log = LoggerFactory.getLogger(getClass.getName)

  protected def request(req:HttpRequest):Future[HttpResponse] = {
    val uri = req.uri.path.toString()
    hasMatch(req) match {
      case Some(actionMetadata:ActionMetadata) =>
        Try(actionMetadata.action(new RequestImpl(req, actionMetadata))) match {
          case Success(f:Future[Response]) => f.map(_.toResponse())
          case Success(r:Response) => Future(r.toResponse())
          case Failure(e:Throwable) => Future(fiveZeroZero(uri, e).toResponse())
      }
      case None => Future(fourZeroFour(uri).toResponse())
    }
  }

  private def hasMatch(req:HttpRequest):Option[ActionMetadata] = {
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

  private def fourZeroFour(missingPath:String):Response = {
    NotFound(s"Nobody home at ${missingPath}.").build()
  }

  private def fiveZeroZero(errorPath:String, error:Throwable):Response = {
    Error(errorPath, error).build()
  }

  private implicit def strToBytes(msg:String):Array[Byte] = {
    msg.getBytes("utf-8")
  }

  def addActions(routes:Routes):Unit = {
    var raw = Map[String, Map[String, Action]]()

    routes.routes.foreach(ctrl => {
      val root = ctrl._1
      val controller = ctrl._2

      raw = raw ++ Map("GET" -> copyAndImproveMap(root, controller.gets, raw.getOrElse("GET", Map[String, Action]())))
      raw = raw ++ Map("POST" -> copyAndImproveMap(root, controller.posts, raw.getOrElse("POST", Map[String, Action]())))
      raw = raw ++ Map("PUT" -> copyAndImproveMap(root, controller.puts, raw.getOrElse("PUT", Map[String, Action]())))
      raw = raw ++ Map("DELETE" -> copyAndImproveMap(root, controller.deletes, raw.getOrElse("DELETE", Map[String, Action]())))
    })

    var regexilized = Map[String, Map[Regex, ActionMetadata]]()

    raw.foreach(actions => {
      val method = actions._1
      val methodActions = actions._2

      regexilized = regexilized ++ Map(method -> mapToRegex(methodActions, Map[Regex, ActionMetadata]()))
    })

    regexilized.foreach {
      case (path, routes) => {
        val existingRoutes = actions.getOrElse(path, Map()) ++ routes
        actions = actions ++ Map(path -> existingRoutes)
      }
    }
  }

  private def copyAndImproveMap(root:String, source:Map[String, Action], target:Map[String, Action]):Map[String, Action] = {
    var copy = target

    source.foreach(actions => {
      log.info("Adding new path: {}", root+actions._1)
      copy = copy ++ Map(root+actions._1 -> actions._2)
    })

    copy
  }

  private def mapToRegex(source:Map[String, Action], target:Map[Regex, ActionMetadata]):Map[Regex, ActionMetadata] = {
    var copy = target
    source.foreach(act => {
      val metadata = createMetadata(act._1, act._2)
      copy = copy ++ Map(metadata.regex -> metadata)
    })

    copy
  }

  private def createMetadata(uri:String, action:Action):ActionMetadata = {
    val findPathParamsRegex = ":([a-zA-Z0-9]+)".r
    val pathParamsRegex = "(:[a-zA-Z0-9]+)".r
    val pathNames = findPathParamsRegex.findAllIn(uri).map(m => m.substring(1)).toList
    val pathRegex = s"${pathParamsRegex.replaceAllIn(uri, "([a-zA-Z0-9]+)")}".r

    new ActionMetadata(uri, action, pathRegex, pathNames)
  }
}

case class ActionMetadata(path:String, action:Action, regex:Regex, keyNames:List[String])
