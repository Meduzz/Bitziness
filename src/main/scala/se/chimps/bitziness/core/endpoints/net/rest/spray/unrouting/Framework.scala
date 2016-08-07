package se.chimps.bitziness.core.endpoints.net.rest.spray.unrouting

import akka.actor.ActorRef

object Framework {

  trait Controller {
    private[rest] var actionDefinitions = List[ActionDefinition]()

    def apply(endpoint:ActorRef)

    def get(uri:String, action:Action, paramex:Map[String, String] = Map()) = http("GET", uri, action, paramex)
    def post(uri:String, action:Action, paramex:Map[String, String] = Map()) = http("POST", uri, action, paramex)
    def put(uri:String, action:Action, paramex:Map[String, String] = Map()) = http("PUT", uri, action, paramex)
    def delete(uri:String, action:Action, paramex:Map[String, String] = Map()) = http("DELETE", uri, action, paramex)

    /**
     * Use this if your verb are not supported. But be warned, you are responsible for the correct response.
     * @param verb the capitalized http verb (ex. GET)
     * @param uri the uri /some/:uri
     * @param action the action to execute when a request matches the uri.
     * @param paramex this allows you to hook up your own regex to match parameters (ex. param1 -> ([0-9]+) to only catch digits...
     */
    def http(verb:String, uri:String, action:Action, paramex:Map[String, String] = Map()) = actionDefinitions = actionDefinitions ++ List(new ActionDefinition(verb, uri, action, paramex))

    implicit def str2Bytes(data:String):Array[Byte] = data.getBytes("utf-8")
    implicit def bytes2Str(data:Array[Byte]):Option[String] = data.length match {
      case 0 => None
      case _ => Some(new String(data, "utf-8"))
    }
  }
}
