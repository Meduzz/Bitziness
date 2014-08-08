package se.chimps.bitziness.core.service.plugins.rest.unrouting

import akka.actor.ActorRef
import spray.http.{HttpMethods, HttpHeader, HttpRequest, HttpResponse}

import scala.util.matching.Regex

object Spray {

  trait Action extends (HttpRequest => HttpResponse) {
    def apply() = this
  }

  object Action {
    def apply(func:(HttpRequest)=>HttpResponse):Action = new Action {
      override def apply(req: HttpRequest): HttpResponse = func(req)
    }
  }

  trait Module {
    private[unrouting] var gets = Map[Regex, Metadata]()
    private[unrouting] var posts = Map[Regex, Metadata]()
    private[unrouting] var puts = Map[Regex, Metadata]()
    private[unrouting] var deletes = Map[Regex, Metadata]()

    def apply(service:ActorRef)

    def get(url:String, action:Action) = gets = gets ++ regexify(url, action)
    def post(url:String, action:Action) = posts = posts ++ regexify(url, action)
    def put(url:String, action:Action) = puts = puts ++ regexify(url, action)
    def delete(url:String, action:Action) = deletes = deletes ++ regexify(url, action)

    def header(name:String, req:HttpRequest):Option[HttpHeader] = {
      req.headers.find(f => f.is(name))
    }

    private def regexify(path:String, action:Action):Map[Regex, Metadata] = {
      val findPathParamsRegex = ":([a-zA-Z0-9]+)".r
      val pathParamsRegex = "(:[a-zA-Z0-9]+)".r
      val pathNames = findPathParamsRegex.findAllIn(path).toList
      val pathRegex = s"^${pathParamsRegex.replaceAllIn(path, "([a-zA-Z0-9]+)")}${"$"}".r
      val metadata = new Metadata(action, pathNames)
      Map(pathRegex -> metadata)
    }
  }

  trait ModuleReader { module:Module =>

    def request(req:HttpRequest):HttpResponse = {
      val uri = req.uri.path.toString()

      val actionWalker = req.method match {
        case HttpMethods.GET => ActionWalker(module.gets)
        case HttpMethods.POST => ActionWalker(module.posts)
        case HttpMethods.PUT => ActionWalker(module.puts)
        case HttpMethods.DELETE => ActionWalker(module.deletes)
      }

      actionWalker.applyOrElse(req, (req:HttpRequest)=>HttpResponse(404))
    }
  }

  case class Metadata(action:Action, keyNames:List[String])
  
  trait ActionWalker extends PartialFunction[HttpRequest, HttpResponse] {
    
  }
 
  object ActionWalker {
    def apply(actions:Map[Regex, Metadata]):ActionWalker = new ActionWalker {
      override def isDefinedAt(x: HttpRequest): Boolean = {
        // TODO throw an exception when this returns more than 1.
        filter(x.uri.path.toString(), actions.keys).length == 1
      }

      override def apply(req: HttpRequest): HttpResponse = {
        val matches = filter(req.uri.path.toString(), actions.keys)

        val metadata = actions(matches(0))
        metadata.action(req)
      }

      def filter(path:String, actionKeys:Iterable[Regex]):List[Regex] = {
        actionKeys.filter(regex =>
          path.matches(regex.regex)
        ).toList
      }
    }
  }
}
