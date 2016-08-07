package se.chimps.bitziness.core.endpoints.net.io

import java.net.InetSocketAddress
import java.nio.ByteOrder

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.io.Tcp.CommandFailed
import akka.testkit.{TestKitBase, TestProbe}
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class LessSandboxyIOTest extends FunSuite with TestKitBase with BeforeAndAfterAll {
  lazy implicit val system = ActorSystem("IoTest")

  test("first client can do ze stuff") {
    val serverProbe = TestProbe()
    val clientProbe = TestProbe()
    val server = system.actorOf(Props(classOf[Server], serverProbe.ref))
    val client = system.actorOf(Props(classOf[Client], clientProbe.ref))

    server ! BindCommand(new InetSocketAddress(8911))
    client ! ConnectCommand(new InetSocketAddress(8911))

    val clientSignIn = clientProbe.expectMsgClass(classOf[SignIn])
    val serverSignIn = serverProbe.expectMsgClass(classOf[SignIn])

    clientSignIn.connection ! Send("test")

    val serverResponse = clientProbe.expectMsgClass(classOf[String])
    assert(serverResponse.equals("test"))

    serverSignIn.connection ! Send("test")
    val clientResponse = serverProbe.expectMsgClass(classOf[String])
    assert(clientResponse.equals("test"))

    client ! DisconnectCommand
    server ! UnbindCommand
  }

  override protected def afterAll(): Unit = {
    system.terminate()
  }
}

case class SignIn(connection:ActorRef)

class Server(override val service:ActorRef) extends ServerIOEndpoint(service) {
  override def onConnection(remote: InetSocketAddress, connection: ActorRef): Option[ActorRef] = {
    Some(context.system.actorOf(Props(classOf[ServerConnection], connection, service)))
  }

  override def onCommandFailed(cmd: CommandFailed): Unit = {
    println("Some command failed in server.")
  }
}

class ServerConnection(override val connection:ActorRef, val service:ActorRef) extends ServerIOConnection {
  implicit val byteOrder = ByteOrder.nativeOrder()

  override def onData(data: ByteString):Unit = {
    val it = data.iterator

    val method = it.getInt
    val rest = it.toByteString

    if (method > 0) {
      val b = ByteString.newBuilder

      b.putInt(0)
      b.append(rest)

      write(b.result())
    } else {
      service ! rest.utf8String
      Unit
    }
  }

  override def onCommandFailed(cmd: CommandFailed): Unit = println("Some command failed in serverConnection.")

  override def onClose(): Unit = context stop self

  override def otherConnectionLogic: Receive = {
    case Send(text) => {
      val b = ByteString.newBuilder

      b.putInt(1)
      b.putBytes(text.getBytes("utf-8"))

      write(b.result())
    }
  }

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = service ! SignIn(self)
}

class Client(override val service:ActorRef) extends ClientIOEndpoint(service) {
  override def onConnection(connection: ActorRef): ActorRef = {
    context.system.actorOf(Props(classOf[ClientConnection], connection, service))
  }

  override def onCommandFailed(cmd: CommandFailed): Unit = {
    println(s"A command failed in the SomeRpcClient. (${cmd.cmd})")
  }
}

class ClientConnection(val connection:ActorRef, val service:ActorRef) extends ClientIOConnection {
  implicit val byteOrder = ByteOrder.nativeOrder()

  override def onData(data: ByteString):Unit = {
    val it = data.iterator

    val method = it.getInt
    val rest = it.toByteString

    if (method > 0) {
      val b = ByteString.newBuilder

      b.putInt(0)
      b.append(rest)

      write(b.result())
    } else {
      service ! rest.utf8String
      Unit
    }
  }

  override def onCommandFailed(cmd: CommandFailed): Unit = {
    println("Some command failed in the client connection.")
  }

  override def onClose(): Unit = context stop self

  override def otherConnectionLogic: Receive = {
    case Send(text) => {
      val b = ByteString.newBuilder

      b.putInt(1)
      b.putBytes(text.getBytes("utf-8"))

      write(b.result())
    }
  }

  @throws[Exception](classOf[Exception])
  override def preStart(): Unit = service ! SignIn(self)
}