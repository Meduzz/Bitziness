package se.chimps.bitziness.core.endpoints.io.protocols.somerpc

import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.testkit.{TestKitBase, TestProbe}
import akka.util.{ByteString, Timeout}
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import se.chimps.bitziness.core.endpoints.io.protocols.somerpc.Common.{NewConnection, RpcRequest, RpcResponse}
import se.chimps.bitziness.core.endpoints.io.{BindCommand, ConnectCommand, DisconnectCommand}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
 *
 */
class SomeRpcEndpointTest extends FunSuite with TestKitBase with BeforeAndAfterAll {
  implicit lazy val system = ActorSystem("SomeRpcEndpoints")
  implicit val timeout = Timeout(3, TimeUnit.SECONDS)

  val reqHandler:((Int, UUID, Seq[ByteString])) => Future[(UUID, Seq[ByteString])] = { tuple =>
    Future((tuple._2, tuple._3))
  }

  val default = Duration(3l, TimeUnit.SECONDS)

  val serverProbe = TestProbe()
  val server = system.actorOf(Props(classOf[SomeRpcServerEndpoint], serverProbe.ref, reqHandler))
  server ! BindCommand(new InetSocketAddress(5432))
  
  val clientProbe = TestProbe()
  val client = system.actorOf(Props(classOf[SomeRpcClientEndpoint], clientProbe.ref, reqHandler))

  test("client connecting and getting its message back in its face") {
    client ! ConnectCommand(new InetSocketAddress(5432))

    val uuid = UUID.randomUUID()
    val data = Seq(ByteString.apply("hello".getBytes("utf-8")))

    val serverConn = serverProbe.expectMsgClass(classOf[NewConnection])
    val connection = clientProbe.expectMsgClass(classOf[NewConnection])
    val futureResp = (connection.connection ? RpcRequest(1, data)).mapTo[RpcResponse]
    val resp = Await.result[RpcResponse](futureResp, default)

    assert(resp.data.equals(data))
  }

  test("server send stuff and gets it back it its face") {
    client ! ConnectCommand(new InetSocketAddress(5432))

    val uuid = UUID.randomUUID()
    val data = Seq(ByteString.apply("hello".getBytes("utf-8")))

    val serverConn = serverProbe.expectMsgClass(classOf[NewConnection])
    val connection = clientProbe.expectMsgClass(classOf[NewConnection])
    val futureResp = (serverConn.connection ? RpcRequest(1, data)).mapTo[RpcResponse]
    val resp = Await.result[RpcResponse](futureResp, default)

    assert(resp.data.equals(data))
  }

  test("disconnecting and stuff") {
    client ! ConnectCommand(new InetSocketAddress(5432))

    val uuid = UUID.randomUUID()
    val data = Seq(ByteString.apply("hello".getBytes("utf-8")))

    val serverConn = serverProbe.expectMsgClass(classOf[NewConnection])
    val connection = clientProbe.expectMsgClass(classOf[NewConnection])

    connection.connection ! DisconnectCommand()
    serverProbe.expectNoMsg(default)

    val futureResp = (connection.connection ? RpcRequest(1, data)).mapTo[RpcResponse]
    val resp = Await.ready[RpcResponse](futureResp, default)
    assert(resp.value.get.isFailure)
  }

  override protected def afterAll(): Unit = system.shutdown()
}
