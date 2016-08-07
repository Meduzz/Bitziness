package se.chimps.bitziness.core.endpoints.net.io

import java.net.InetSocketAddress
import java.nio.ByteOrder

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.io.Tcp._
import akka.testkit.{TestKitBase, TestProbe}
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import se.chimps.bitziness.core.endpoints.net.io.Common.ConnectionBase

class IOEndpointTest extends FunSuite with TestKitBase with ByteStringUtil with BeforeAndAfterAll {
  lazy implicit val system = ActorSystem("IoEndpoint")

  test("server connection does what it's told") {
    val probe = TestProbe()
    val con = system.actorOf(Props(classOf[MyServerConnection], probe.ref))

    con ! Send("test")
    val write = probe.expectMsgClass(classOf[Write])
    assertWrite(write.data, 1, "test")

    probe.send(con, Received(send("test")))
    val reply = probe.expectMsgClass(classOf[Write])
    assertWrite(reply.data, 0, "test")
  }

  test("client connection does what it's told") {
    val probe = TestProbe()
    val con = system.actorOf(Props(classOf[MyClientConnection], probe.ref))

    con ! Send("test")
    val write = probe.expectMsgClass(classOf[Write])
    assertWrite(write.data, 1, "test")

    probe.send(con, Received(send("test")))
    val reply = probe.expectMsgClass(classOf[Write])
    assertWrite(reply.data, 0, "test")
  }

  test("all in testing the io package") {
    val serverProbe = TestProbe()
    val clientProbe = TestProbe()

    val server = system.actorOf(Props(classOf[MyServerEndpoint], serverProbe.ref))
    val client = system.actorOf(Props(classOf[MyClientEndpoint], clientProbe.ref))

    server ! BindCommand(new InetSocketAddress(8910))

    clientProbe.send(server, Connected(new InetSocketAddress(5432), new InetSocketAddress(8910)))
    val register = clientProbe.expectMsgClass(classOf[Register])

    clientProbe.send(register.handler, Received(send("test")))
    val reply = clientProbe.expectMsgClass(classOf[Write])

    assertWrite(reply.data, 0, "test")

    serverProbe.send(client, Connected(new InetSocketAddress(5432), new InetSocketAddress(8910)))
    val clientRegister = serverProbe.expectMsgClass(classOf[Register])

    serverProbe.send(clientRegister.handler, Received(send("test")))
    val clientReply = serverProbe.expectMsgClass(classOf[Write])

    assertWrite(clientReply.data, 0, "test")

    // TODO improve this bit of the test... we're not expecting any errors.
    client ! ConnectCommand(new InetSocketAddress(8910))
    val connection = clientProbe.expectMsgClass(classOf[ActorRef])
    connection ! Send("spam")
  }

  def assertWrite(data:ByteString, flag:Int, text:String) = {
    implicit val byteOrder = ByteOrder.nativeOrder()
    val it = data.iterator
    val m = it.getInt
    val rest = it.toByteString.utf8String

    assert(m == flag, "Flags did not match.")
    assert(rest.equals(text), "Text did not match.")
  }

  override protected def afterAll(): Unit = system.terminate()
}

case class Send(text:String)

class MyServerConnection(val connection:ActorRef) extends ServerIOConnection with Logic {

  override def onCommandFailed(cmd:CommandFailed): Unit = {
    println("A command failed.")
  }

  override def onClose(): Unit = {
    context stop self
  }

  override def otherConnectionLogic: Receive = {
    case Send(text) => write(send(text))
  }
}

class MyClientConnection(val connection:ActorRef) extends ClientIOConnection with Logic {
  override def onClose(): Unit = context stop self

  override def onCommandFailed(cmd: CommandFailed): Unit = {
    println("A command failed.")
  }

  override def otherConnectionLogic: Receive = {
    case Send(text) => {
      write(send(text))
    }
  }
}

trait Logic extends ByteStringUtil { self:ConnectionBase =>
  override def onData(data: ByteString):Unit = {
    implicit val byteOrder = ByteOrder.nativeOrder()
    val it = data.iterator

    val m = it.getInt
    val rest = it.toByteString

    if (m == 0) {
      Unit
    } else {
      write(doReply(rest))
    }
  }
}

trait ByteStringUtil {
  implicit val byteOrder = ByteOrder.nativeOrder()

  def send(text:String):ByteString = {
    val b = ByteString.newBuilder

    b.putInt(1)
    b.putBytes(text.getBytes("utf-8"))

    b.result()
  }

  def doReply(text:ByteString):ByteString = {
    val b = ByteString.newBuilder

    b.putInt(0)
    b.append(text)

    b.result()
  }
}

class MyServerEndpoint(override val service:ActorRef) extends ServerIOEndpoint(service) {
  override def onConnection(remote: InetSocketAddress, connection: ActorRef): Option[ActorRef] = {
    Some(context.system.actorOf(Props(classOf[MyServerConnection], connection)))
  }

  override def onCommandFailed(cmd: CommandFailed): Unit = {
    println("A server endpoint command failed.")
  }
}

class MyClientEndpoint(override val service:ActorRef) extends ClientIOEndpoint(service) {
  override def onConnection(connection: ActorRef): ActorRef = {
    val created = context.system.actorOf(Props(classOf[MyClientConnection], connection))
    service ! created
    created
  }

  override def onCommandFailed(cmd: CommandFailed): Unit = {
    println("A client endpoint command failed.")
  }
}