package se.chimps.bitziness.core.endpoints.somerpc.client

import java.net.InetSocketAddress
import java.nio.ByteOrder
import java.util.UUID

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.io.Tcp.Received
import akka.util.{ByteIterator, ByteString}
import se.chimps.bitziness.core.Endpoint
import se.chimps.bitziness.core.endpoints.somerpc.common.Model.Transmit
import se.chimps.bitziness.core.endpoints.somerpc.common.{BinaryExtension, Extension, Type}

import scala.util.hashing.MurmurHash3

/**
 *
 */
trait SomeRpcClientEndpoint extends Endpoint {

  def setupSomeRpcClient(builder:ClientBuilder):ActorRef

  val endpoint = setupSomeRpcClient(new ClientBuilderImpl(null, Seq()))
  implicit val system = context.system

}

class SomeRpcDataClientActor(settings:SomeRpcClientSettings) extends Actor {
  implicit val byteOrder = ByteOrder.nativeOrder()

  override def receive: Receive = {
    // TODO replace uuid with promise, create a promise-store and, create a new uuid with every request.
    case Transmit(method, uuid, data) => {
      val pf = buildBinaryExecute // we're calling this once, since it could be expensive.
      val bin = data.map(pf(_))
      val bs = write(method, uuid, bin)
      // TODO send data.
    }
    case Received(data) => {
      read(data) match {
        case Seq(method:Int, flags:Int) => {
          // connection setup.
        }
        case Seq(method:Int, uuid:UUID, data:Seq[Array[Byte]]) => {
          // normal data.
        }
      }
    }
  }

  def getBinaryExtentions():Seq[BinaryExtension] = {
    settings.extensions
      .filter(_.t.equals(Type.BINARY))
      .map(_.asInstanceOf[BinaryExtension])
  }

  def buildBinaryExecute:PartialFunction[Array[Byte], Array[Byte]] = {
    val binary = getBinaryExtentions()
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

trait ClientBuilder {
  def connect(address:InetSocketAddress):ClientBuilder
  def registerExtension(extension:Extension):ClientBuilder
  def registerExtensions(extensions:Seq[Extension]):ClientBuilder
  def build()(implicit system:ActorSystem):ActorRef
}

case class SomeRpcClientSettings(address: InetSocketAddress, extensions:Seq[Extension])

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

  override def build()(implicit system:ActorSystem):ActorRef = {
    system.actorOf(Props(classOf[SomeRpcDataClientActor], SomeRpcClientSettings(this.address, this.extensions)))
  }
}