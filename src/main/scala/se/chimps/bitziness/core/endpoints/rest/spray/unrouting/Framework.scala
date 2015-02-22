package se.chimps.bitziness.core.endpoints.rest.spray.unrouting

import akka.actor.ActorRef
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Model.RequestImpl
import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Model.Responses.{Error, NotFound}
import spray.http._

object Framework {

  trait Controller {
    private[rest] var gets = Map[String, Action]()
    private[rest] var posts = Map[String, Action]()
    private[rest] var puts = Map[String, Action]()
    private[rest] var deletes = Map[String, Action]()

    def apply(endpoint:ActorRef)

    def get(uri:String, action:Action) = gets = gets ++ Map(uri -> action)
    def post(uri:String, action:Action) = posts = posts ++ Map(uri -> action)
    def put(uri:String, action:Action) = puts = puts ++ Map(uri -> action)
    def delete(uri:String, action:Action) = deletes = deletes ++ Map(uri -> action)

    implicit def str2Bytes(data:String):Array[Byte] = {
      data.getBytes("utf-8")
    }
  }

  trait View {
    def render():Array[Byte]
    def contentType:String
  }
}
