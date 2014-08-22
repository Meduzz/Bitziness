package se.chimps.bitziness.core.endpoints.rest.spray.unrouting

import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Model.{Response, Request}

import scala.util.matching.Regex

object Action {
  def apply(uri:String)(func:(Request)=>Response):Action = new Action {
    val meta = metadata()

    override def isDefinedAt(x:Request):Boolean = x.path.equals(uri)

    override def apply(req:Request):Response = func(req)

    private[unrouting] def metadata():ActionMetadata = {
      val findPathParamsRegex = ":([a-zA-Z0-9]+)".r
      val pathParamsRegex = "(:[a-zA-Z0-9]+)".r
      val pathNames = findPathParamsRegex.findAllIn(uri).toList
      val pathRegex = s"^${pathParamsRegex.replaceAllIn(uri, "([a-zA-Z0-9]+)")}${"$"}".r

      new ActionMetadata(pathRegex, pathNames)
    }

    override def matchesUri(uri:String):Boolean = uri.matches(meta.regex.regex)
  }
}

trait Action extends PartialFunction[Request, Response] {
  def meta:ActionMetadata
  def matchesUri(uri:String):Boolean
}

case class ActionMetadata(regex:Regex, keyNames:List[String])
