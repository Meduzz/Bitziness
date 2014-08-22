package se.chimps.bitziness.core.endpoints.rest.spray.unrouting

import akka.actor.ActorRef
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Model.RequestImpl
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Model.Responses.{Error, NotFound}
import spray.http._

object Framework {

  trait Controller {
    private[unrouting] var gets = List[Action]()
    private[unrouting] var posts = List[Action]()
    private[unrouting] var puts = List[Action]()
    private[unrouting] var deletes = List[Action]()

    def apply(service:ActorRef)

    def get(action:Action) = gets = gets ++ List(action)
    def post(action:Action) = posts = posts ++ List(action)
    def put(action:Action) = puts = puts ++ List(action)
    def delete(action:Action) = deletes = deletes ++ List(action)
  }

  trait ModuleReader { module:Controller =>

    def request(req:HttpRequest):HttpResponse = {
      val uri = req.uri.path.toString()

      val action = req.method match {
        case HttpMethods.GET => findMatch(module.gets, uri)
        case HttpMethods.POST => findMatch(module.posts, uri)
        case HttpMethods.PUT => findMatch(module.puts, uri)
        case HttpMethods.DELETE => findMatch(module.deletes, uri)
      }

      action.getOrElse(fourZeroFour(uri))(new RequestImpl(req)).toResponse()
    }

    private def findMatch(actions:List[Action], path:String):Option[Action] = {
      // TODO throw exception when there are more than one action matching.
      actions.find(action => action.matchesUri(path))
    }

    private def fourZeroFour(missingPath:String):Action = Action("") { req =>
      NotFound().withEntity(s"Nobody home at ${missingPath}.").build()
    }

    private def fiveZeroZero(errorPath:String, error:Throwable):Action = Action("") { req =>
      Error().build()
    }

    private implicit def strToBytes(msg:String):Option[Array[Byte]] = {
      Some(msg.getBytes("utf-8"))
    }
  }
}
