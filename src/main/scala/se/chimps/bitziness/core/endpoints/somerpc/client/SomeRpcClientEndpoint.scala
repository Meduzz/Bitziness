package se.chimps.bitziness.core.endpoints.somerpc.client

import java.net.InetSocketAddress
import java.nio.ByteOrder
import java.util.UUID

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.Tcp.{Register, Connected, Received}
import akka.util.{ByteIterator, ByteString}
import se.chimps.bitziness.core.Endpoint
import se.chimps.bitziness.core.endpoints.somerpc.common.Extensions._
import se.chimps.bitziness.core.endpoints.somerpc.common.Model.{Connection, Event, Handle, Transmit}
import se.chimps.bitziness.core.endpoints.somerpc.common._

import scala.concurrent.Promise
import scala.util.hashing.MurmurHash3

/**
 *
 */
trait SomeRpcClientEndpoint extends Endpoint {

  def setupSomeRpcClient(builder:ClientBuilder):ActorRef

  val endpoint = setupSomeRpcClient(new ClientBuilderImpl(null, Seq()))
  implicit val system = context.system

}

private class SomeRpcDataClientActor(settings:SomeRpcClientSettings) extends Actor {
  implicit val byteOrder = ByteOrder.nativeOrder()

  var promisses:Map[UUID, Promise[Seq[Array[Byte]]]] = Map()

  override def receive: Receive = {
    case Transmit(method, promise, data) => {
      val uuid = store(promise)
      val pf = buildBinaryExecute // we're calling this once, since it could be expensive.
      val bin = data.map(pf(_))
      val bs = write(method, uuid, bin)
      // TODO send data.
    }
    case Received(data) => {
      val connection = new Connection(sender())
      read(data) match {
        case Seq(method:Int, flags:Int) => {
          if (method == Methods.CONNECT) {
            // connection setup.
          } else {
            // start screaming!
          }
        }
        case Seq(method:Int, uuid:UUID, data:Seq[Array[Byte]]) => {
          // normal data.
          if (method == Methods.REPLY) {
            load(uuid).foreach(_.success(data))
          } else {
            settings.pf(Handle(method, uuid, data, connection))
          }
        }
      }
    }
    case Connected(remote, local) => {
      sender() ! Register(self)
    }
  }

  def store(promise:Promise[Seq[Array[Byte]]]):UUID = {
    val uuid = UUID.randomUUID()
    promisses = promisses ++ Map(uuid -> promise)
    uuid
  }
  
  def load(uuid: UUID):Option[Promise[Seq[Array[Byte]]]] = {
    promisses.get(uuid)
  }

  def decoders():Seq[Decoder] = {
    settings.extensions
      .filter(_.extensionType.equals(Type.DECODER))
      .map(_.asInstanceOf[Decoder])
  }

  def decoder:PartialFunction[Any, Array[Byte]]= {
    decoders()
      .headOption
      .map(_.execute).getOrElse(new JavaDecoder().execute)
  }

  def encoders():Seq[Encoder] = {
    settings.extensions
      .filter(_.extensionType.equals(Type.ENCODER))
      .map(_.asInstanceOf[Encoder])
  }

  def encoder:PartialFunction[Array[Byte], Any] = {
    encoders()
      .headOption
      .map(_.execute).getOrElse(new JavaEncoder().execute)
  }

  def binaryCodecs():Seq[Binary] = {
    settings.extensions
      .filter(_.extensionType.equals(Type.BINARY))
      .map(_.asInstanceOf[Binary])
  }

  def buildBinaryExecute:PartialFunction[Array[Byte], Array[Byte]] = {
    val binary = binaryCodecs()
    binary.tail.foldLeft(binary.head.execute)((in, out) => in.andThen(out.execute))
  }

  def write(method:Int, uuid:UUID, data:Seq[Array[Byte]]):ByteString = {
    val builder = ByteString.newBuilder

    val uuidAsBytes = uuid.toString.getBytes("utf-8")
    val uuidLen = uuidAsBytes.length

    builder.putInt(method)
    builder.putInt(uuidLen)
    builder.putBytes(uuidAsBytes)
    builder.putInt(data.size)

    data.foreach({ bytes =>
      builder.putInt(bytes.length)
      builder.putInt(MurmurHash3.bytesHash(bytes))
      builder.putBytes(bytes)
    })

    builder.result()
  }

  def read(data:ByteString):Seq[Any] = {
    val it = data.iterator
    val method = it.getInt
    val uuidLen = it.getInt

    if (it.isEmpty) {
      return Seq(method, uuidLen)
    }

    val uuidBytes = Array.ofDim[Byte](uuidLen)
    it.getBytes(uuidBytes)
    val uuid = UUID.fromString(new String(uuidBytes, "utf-8"))

    val blobCount = it.getInt
    val blobs:Seq[Array[Byte]] = readBlobs(it, blobCount)

    Seq(method, uuid, blobs)
  }

  def readBlobs(it:ByteIterator, blobCount:Int):Seq[Array[Byte]] = {
    if (blobCount > 0) {
      val blobLen = it.getInt
      val blobCrc = it.getInt
      val blob: Array[Byte] = Array.ofDim[Byte](blobLen)
      it.getBytes(blob)

      require(MurmurHash3.bytesHash(blob) == blobCrc, s"Invalid crc on blob #$blobCount!")

      Seq(blob) ++ readBlobs(it, blobCount - 1)
    } else {
      Seq()
    }
  }
}

sealed trait ClientBuilder {
  def connect(address:InetSocketAddress):ClientBuilder
  def registerExtension(extension:Extension):ClientBuilder
  def registerExtensions(extensions:Seq[Extension]):ClientBuilder
  def build(pf:PartialFunction[Event, Unit])(implicit system:ActorSystem):ActorRef
}

case class SomeRpcClientSettings(address: InetSocketAddress, extensions:Seq[Extension], pf:PartialFunction[Event, Unit])

private case class ClientBuilderImpl(address: InetSocketAddress, extensions:Seq[Extension]) extends ClientBuilder {
  override def connect(address: InetSocketAddress): ClientBuilder = {
    copy(address)
  }

  override def registerExtension(extension: Extension): ClientBuilder = {
    copy(extensions = this.extensions ++ Seq(extension))
  }

  override def registerExtensions(extensions: Seq[Extension]): ClientBuilder = {
    copy(extensions = this.extensions ++ extensions)
  }

  override def build(pf:PartialFunction[Event, Unit])(implicit system:ActorSystem):ActorRef = {
    system.actorOf(Props(classOf[SomeRpcDataClientActor], SomeRpcClientSettings(this.address, this.extensions, pf.orElse(fallback))))
  }

  private def fallback:PartialFunction[Event, Unit] = {
    case e:Event => _
  }
}