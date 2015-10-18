package se.chimps.bitziness.core.endpoints.http.server.unrouting

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.matching.Regex

/**
 *
 */
trait Controller {
  private[unrouting] var definitions:List[ActionDefinition] = List()

  def get(path:String, action:Action, paramex:Map[String, String] = Map()) = addAction("GET", path, action, paramex)
  def post(path:String, action:Action, paramex:Map[String, String] = Map()) = addAction("POST", path, action, paramex)
  def put(path:String, action:Action, paramex:Map[String, String] = Map()) = addAction("PUT", path, action, paramex)
  def delete(path:String, action:Action, paramex:Map[String, String] = Map()) = addAction("DELETE", path, action, paramex)
  def option(path:String, action:Action, paramex:Map[String, String] = Map()) = addAction("OPTION", path, action, paramex)

  protected def addAction(method:String, path:String, action:Action, paramex:Map[String, String]) = {
    val findPathParamsRegex = ":([a-zA-Z0-9]+)".r
    val pathParamsRegex = "(:[a-zA-Z0-9]+)".r
    val pathNames = findPathParamsRegex.findAllIn(path).map(m => m.substring(1)).toList
    val pathRegex = pathNames.foldLeft(path)((url, name) => s"(:$name)".r.replaceAllIn(url, paramex.getOrElse(name, "([a-zA-Z0-9]+)"))).r

    definitions = definitions ++ List(ActionDefinition(method, pathRegex, action, pathNames))
  }
}

object Action {

  def apply(func:(UnroutingRequest)=>Future[HttpResponse]):Action = new Action {
    override def apply(req:UnroutingRequest):Future[HttpResponse] = func(req)
  }

  def apply(func:() => Future[HttpResponse]):Action = new Action {
    override def apply(req:UnroutingRequest):Future[HttpResponse] = func()
  }

  def sync(func:(UnroutingRequest)=>HttpResponse):Action = new Action {
    override def apply(req:UnroutingRequest):Future[HttpResponse] = Future(func(req))
  }

  def sync(func:() => HttpResponse):Action = new Action {
    override def apply(req:UnroutingRequest):Future[HttpResponse] = Future(func())
  }
}

trait Action extends (UnroutingRequest => Future[HttpResponse]) {
  def apply() = this
}

case class ActionDefinition(method:String, pathRegex:Regex, action:Action, paramNames:List[String])
case class UnroutingRequest(raw:HttpRequest, params:Map[String, String], mat: Materializer) extends SugarCoating {
  implicit val materializer = mat
}