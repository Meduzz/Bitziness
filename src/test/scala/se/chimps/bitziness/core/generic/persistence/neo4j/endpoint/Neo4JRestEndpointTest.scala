package se.chimps.bitziness.core.generic.persistence.neo4j.endpoint

import akka.actor.Status.Failure
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.PipeToSupport
import akka.testkit.{TestKitBase, TestProbe}
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import se.chimps.bitziness.core.endpoints.http.ConnectionBuilder

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.Future

class Neo4JRestEndpointTest extends FunSuite with TestKitBase with BeforeAndAfterAll {
  import TestModel._

  implicit lazy val system = ActorSystem("neo4j-rest")

  val service = TestProbe()
  val endpoint = system.actorOf(Props(classOf[TestNeo4JEndpoint], service.ref))

  test("create a node and remove it") {
    endpoint ! Create(None, Map("property1" -> "Test"))

    val node = service.expectMsgType[Node]
    assert(node != null)
    assert(node.id > 0)

    endpoint ! DeleteNode(node)
    service.expectMsgType[Unit]

    endpoint ! GetNode(node)
    service.expectMsgType[Failure]
  }

  test("create 2 nodes, link them then delete the whole shebang") {
    endpoint ! Create(Some("PERSON"), Map("title" -> "Mr", "lastname" -> "Spammer"))
    val node1 = service.expectMsgType[Node]

    endpoint ! Create(Some("PERSON"), Map("title" -> "Mrs", "lastname" -> "Spammer"))
    val node2 = service.expectMsgType[Node]

    endpoint ! Link(node1, node2, "FAMILY", Map("kids" -> "2"))
    val relation1 = service.expectMsgType[Relationship]

    endpoint ! GetNode(node1)
    val node1Details = service.expectMsgType[NodeDetails]

    endpoint ! GetNode(node2)
    val node2Details = service.expectMsgType[NodeDetails]

    endpoint ! GetRelationship(relation1)
    val relation1Details = service.expectMsgType[RelationshipDetails]

    endpoint ! DeleteNode(node1)
    service.expectMsgType[Failure]

    endpoint ! DeleteRelationship(relation1)
    service.expectMsgType[Unit]

    endpoint ! DeleteNode(node2)
    service.expectMsgType[Unit]

    endpoint ! DeleteNode(node1)
    service.expectMsgType[Unit]

    assert(node1Details.labels.contains("PERSON"))
    assert(node2Details.labels.contains("PERSON"))
    assert(relation1Details.relation.equals("FAMILY"))
    assert(relation1Details.start.equals(node1))
    assert(relation1Details.end.equals(node2))
    assert(node1Details.data("lastname").equals("Spammer"))
    assert(node2Details.data("lastname").equals("Spammer"))
  }

  test("algorithms are teh shitz") {
    endpoint ! Create(Some("PERSON"), Map("a" -> "b"))
    val p1 = service.expectMsgType[Node]

    endpoint ! Create(Some("PERSON"), Map("c" -> "d"))
    val p2 = service.expectMsgType[Node]

    endpoint ! Link(p1, p2, "KNOWS", Map())
    val knows1 = service.expectMsgType[Relationship]

    endpoint ! Create(Some("PERSON"), Map("e" -> "f"))
    val p3 = service.expectMsgType[Node]

    endpoint ! Link(p2, p3, "KNOWS", Map())
    val knows2 = service.expectMsgType[Relationship]

    endpoint ! UseAlgorithm(p1, p3, "shortestPath", "3")
    val paths = service.expectMsgType[Seq[Path]]

    assert(paths.length == 1)
    val path = paths.head
    assert(path.start.equals(p1))
    assert(path.end.equals(p3))
    assert(path.relations(0).equals(knows1))
    assert(path.relations(1).equals(knows2))
    assert(path.length == 2)
  }

  override protected def afterAll(): Unit = system.terminate()
}

object TestModel {
  case class Create(label:Option[String], data:Map[String, AnyRef])
  case class Link(start:Node, end:Node, typ:String, data:Map[String, AnyRef])
  case class DeleteRelationship(relationship:Relationship)
  case class DeleteNode(node:Node)
  case class GetNode(node:Node)
  case class GetRelationship(relationship: Relationship)
  case class UseAlgorithm(start:Node, end:Node, name:String, special:String)
}

class TestNeo4JEndpoint(val service:ActorRef) extends Neo4JRestEndpoint with PipeToSupport {
  import TestModel._

  implicit val ec = Implicits.global

  override def username: Option[String] = Some("neo4j")
  override def password: String = "sp4msp4m"
  override def host: String = "192.168.235.20"
  override def port: Int = 8474

  override def setupConnection(builder: ConnectionBuilder): ActorRef = {
    builder.host(host, port).build(false)
  }

  override def receive: Receive = {
    case Create(label, data) => {
      val node = for {
        nodeFuture <- createNode(data)
        labelFuture <- optionLabelToFuture(nodeFuture, label)
      } yield nodeFuture

      node.pipeTo(service)
    }
    case Link(start, end, typ, data) => {
      createRelation(start, end, typ, data).pipeTo(service)
    }
    case DeleteRelationship(relationship) => {
      deleteRelation(relationship).pipeTo(service)
    }
    case DeleteNode(node) => {
      deleteNode(node).pipeTo(service)
    }
    case GetNode(node) => {
      getNode(node).pipeTo(service)
    }
    case GetRelationship(relationship) => {
      getRelation(relationship).pipeTo(service)
    }
    case UseAlgorithm(start, end, algo, special) => {
      execute(start, end, builder => {
        val relBuilder = algo match {
          case "allPaths" => builder.allPath(special.toInt)
          case "shortestPath" => builder.shortestPath(special.toInt)
          case "allSimplePart" => builder.allSimplePath(special.toInt)
          case "dijkstra" => builder.dijkstra(special)
        }
        relBuilder.relation("FAMILY", Directions.OUT).build(false)
      }).pipeTo(service)
    }
  }

  def optionLabelToFuture(node:Node, label:Option[String]):Future[Unit] = Future {
    label.map(lab => addLabel(node, Seq(lab)))
  }
}