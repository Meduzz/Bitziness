package se.chimps.bitziness.core.endpoints.persistence.neo4j.endpoint

import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.{Authorization, BasicHttpCredentials}
import akka.util.ByteString
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import se.chimps.bitziness.core.endpoints.net.http.HttpClientEndpoint
import se.chimps.bitziness.core.endpoints.net.http.client.RequestBuilders

import scala.concurrent.{ExecutionContext, Future}

trait Neo4JRestEndpoint extends HttpClientEndpoint with RequestBuilders with NodeOperations with RelationOperations with DegreeOperations with UserOperation with AlgorithmOperation {

  implicit val formats = DefaultFormats
  implicit def ec:ExecutionContext

  def username:Option[String] = None
  def password:String
  def host:String
  def port:Int

  val rootUrl:String = "/db/data"

  val nodeUrl = "/node"
  val relationUrl = "/relation"

  def auth(req:HttpRequest):HttpRequest = {
    username match {
      case Some(user) => req.withHeaders(Authorization(BasicHttpCredentials(user, password)))
      case None => req
    }
  }

  def map2Json(data:Map[String, AnyRef]):RequestEntity = {
    HttpEntity(ContentTypes.`application/json`, write(data))
  }

  def entityToString(res:HttpResponse):Future[String] = {
    if (res.status.intValue() == 404) {
      res.entity.dataBytes.runFold(ByteString.empty)((a, b) => a.concat(b)).map(_.utf8String).map(str => {
        val json = parse(str)
        val msg = (json \ "cause" \ "message").extract[String]
        throw new NodeNotFoundException(msg)
      })
    } else if (res.status.intValue() == 409) {
      res.entity.dataBytes.runFold(ByteString.empty)((a, b) => a.concat(b)).map(_.utf8String).map(str => {
        val json = parse(str)
        val msg = (json \ "errors" \ "message").extract[String]
        throw new NodeHasRelationsException(msg)
      })
    } else {
      res.entity.dataBytes.runFold(ByteString.empty)((a, b) => a.concat(b)).map(_.utf8String)
    }
  }
}

trait NodeOperations { self:Neo4JRestEndpoint =>
  def createNode(data:Map[String, AnyRef] = Map()):Future[Node] = {
    send(auth(POST(s"$rootUrl$nodeUrl", Some(map2Json(data)))))
      .map(entityToString)
      .flatMap[String](a => a)
      .map(json => {
        Node((parse(json) \ "metadata" \ "id").extract[Long])
      })
  }

  def updateNode(node:Node, data:Map[String, AnyRef]):Future[Unit] = {
    send(auth(PUT(s"$rootUrl${node.url}/properties", Some(map2Json(data)))))
      .map(entityToString)
      .map(_ => Unit)
  }

  def deleteNode(node:Node):Future[Unit] = {
    send(auth(DELETE(s"$rootUrl${node.url}")))
      .map(entityToString)
      .flatMap[String](a => a)
      .map(_ => Unit)
  }

  def getNode(node:Node):Future[NodeDetails] = {
    send(auth(GET(s"$rootUrl${node.url}")))
      .map(entityToString)
      .flatMap[String](a => a)
      .map(str => {
        val json = parse(str)
        val id = (json \ "metadata" \ "id").extract[Long]
        val labels = (json \ "metadata" \ "labels").extract[Seq[String]]
        val data = (json \ "data").asInstanceOf[JObject].values

        NodeDetails(id, labels, data)
      })
  }

  def addLabel(node:Node, labels:Seq[String]):Future[Unit] = {
    send(auth(POST(s"$rootUrl${node.url}/labels", Some(write(labels)))))
      .map(entityToString)
      .map(_ => Unit)
  }
}

trait RelationOperations { self:Neo4JRestEndpoint =>
  def createRelation(start:Node, end:Node, relation:String, data:Map[String, AnyRef]):Future[Relationship] = {
    val obj = RelationBody(s"http://$host:$port$rootUrl${end.url}", relation, data)
    send(auth(POST(s"$rootUrl${start.url}/relationships", Some(write(obj)))))
      .map(entityToString)
      .flatMap[String](a => a)
      .map(str => {
        val json = parse(str)
        val id = (json \ "metadata" \ "id").extract[Long]
        Relationship(id)
      })
  }

  def getRelation(relationship:Relationship):Future[RelationshipDetails] = {
    send(auth(GET(s"$rootUrl${relationship.url}")))
      .map(entityToString)
      .flatMap[String](a => a)
      .map(str => {
        val json = parse(str)
        val id = (json \ "metadata" \ "id").extract[Long]
        val typ = (json \ "metadata" \ "type").extract[String]
        val data = (json \ "data").asInstanceOf[JObject].values
        val start = (json \ "start").extract[String]
        val end = (json \ "end").extract[String]

        RelationshipDetails(id.toLong, Node.fromUrl(start), Node.fromUrl(end), typ, data)
      })
  }

  def updateRelation(relationship:Relationship, data:Map[String, AnyRef]):Future[Unit] = {
    send(auth(PUT(s"$rootUrl${relationship.url}/properties", Some(map2Json(data)))))
      .map(entityToString)
      .map(_ => Unit)
  }

  def deleteRelation(relationship:Relationship):Future[Unit] = {
    send(auth(DELETE(s"$rootUrl${relationship.url}")))
      .map(entityToString)
      .flatMap[String](a => a)
      .map(_ => Unit)
  }
}

trait DegreeOperations { self:Neo4JRestEndpoint =>
  def getDegree(node:Node, direction:Direction, relation:String):Future[Long] = {
    send(auth(GET(s"$rootUrl${node.url}/${direction.value}/$relation")))
      .map(entityToString)
      .flatMap[String](a => a)
      .map(_.toLong)
  }
}

trait UserOperation { self:Neo4JRestEndpoint =>
  def changePassword(user:String, password:String):Future[Unit] = {
    val data = Map("password" -> password)
    send(auth(POST(s"/user/$user/password", Some(map2Json(data)))))
      .map(entityToString)
      .map(_ => Unit)
  }
}

trait AlgorithmOperation { self:Neo4JRestEndpoint =>
  def execute(start:Node, end:Node, func:(AlgoritmBuilder)=>PathQuery):Future[Seq[Path]] = {
    send(start, func(PathQuery(end, "", false, 0, "", "", Directions.OUT)))
  }

  protected def queryToJson(query:PathQuery):String = {
    val optional:JObject = if (query.algo == "djikstra") ("cost_property" -> query.cost) else ("max_depth" -> query.depth)
    val json = ("to" -> s"$rootUrl${query.end.url}") ~
      optional ~
      ("relationship" ->
          ("type" -> query.typ) ~
          ("direction" -> query.direction.value)
      ) ~
      ("algorithm" -> query.algo)

    compact(render(json))
  }

  protected def send(start:Node, query:PathQuery):Future[Seq[Path]] = {
    val path = if (query.multiple) { "/paths" } else { "/path" }
    send(auth(POST(s"$rootUrl${start.url}$path", Some(queryToJson(query)))))
      .map(entityToString)
      .flatMap[String](s => s)
      .map(str => {
        val json = parse(str)

        json match {
          case obj:JObject => Seq(parsePath(obj))
          case arr:JArray => arr.children.map(obj => parsePath(obj)).toSeq
          case _ => Seq()
        }
      })
  }

  protected def parsePath(json:JValue):Path = {
    val directions:Seq[String] = (json \\ "directions").extract[Seq[String]]
    val someWeight:Option[Int] = (json \\ "weight").extractOpt[Int]
    val start:String = (json \\ "start").extract[String]
    val nodes:Seq[String] = (json \\ "nodes").extract[Seq[String]]
    val length:Int = (json \\ "length").extract[Int]
    val relations:Seq[String] = (json \\ "relationships").extract[Seq[String]]
    val end:String = (json \\ "end").extract[String]

    val weigth = BigDecimal(someWeight.getOrElse(0))
    val startNode = Node.fromUrl(start)
    val endNode = Node.fromUrl(end)
    val relationships = relations.map(Relationship.fromUrl)
    val realNodes = nodes.map(Node.fromUrl)

    Path(directions, weigth, startNode, realNodes, length, relationships, endNode)
  }
}

case class Node(id:Long) {
  def url:String = s"/node/$id"
}
case class Relationship(id:Long) {
  def url:String = s"/relationship/$id"
}
case class NodeDetails(id:Long, labels:Seq[String], data:Map[String, Any])
case class RelationshipDetails(id:Long, start:Node, end:Node, relation:String, data:Map[String, Any])
case class Direction(value:String)
case class RelationBody(to:String, `type`:String, data:Map[String, AnyRef])
case class Path(directions:Seq[String], weight:BigDecimal, start:Node, nodes:Seq[Node], length:Int, relations:Seq[Relationship], end:Node)

class NodeNotFoundException(val message:String) extends Exception(message)
class NodeHasRelationsException(val message:String) extends Exception(message)

object Directions {
  val IN = Direction("IN")
  val OUT = Direction("OUT")
}

object Node {
  def fromUrl(url:String):Node = {
    val tail = url.split("/").reverse.head
    Node(tail.toLong)
  }
}

object Relationship {
  def fromUrl(url:String):Relationship = {
    val tail = url.split("/").reverse.head
    Relationship(tail.toLong)
  }
}

trait AlgoritmBuilder {
  def shortestPath(depth:Int):RelationBuilder
  def allSimplePath(depth:Int):RelationBuilder
  def allPath(depth:Int):RelationBuilder
  def dijkstra(costProperty:String):RelationBuilder
}

trait RelationBuilder {
  def relation(typ:String, direction: Direction):RelationBuilder
  def build(multiple:Boolean):PathQuery
}

case class PathQuery(end:Node, algo:String, multiple:Boolean, depth:Int, cost:String, typ:String, direction: Direction) extends AlgoritmBuilder with RelationBuilder {

  override def shortestPath(depth: Int): RelationBuilder = copy(end, "shortestPath", multiple, depth, cost, typ, direction)

  override def allPath(depth: Int): RelationBuilder = copy(end, "allPaths", multiple, depth, cost, typ, direction)

  override def dijkstra(costProperty: String): RelationBuilder = copy(end, "dijkstra", multiple, depth, cost, typ, direction)

  override def allSimplePath(depth: Int): RelationBuilder = copy(end, "allSimplePaths", multiple, depth, cost, typ, direction)

  override def relation(typ: String, direction: Direction): RelationBuilder = copy(end, algo, multiple, depth, cost, typ, direction)

  override def build(multiple: Boolean): PathQuery = copy(end, algo, multiple, depth, cost, typ, direction)

}