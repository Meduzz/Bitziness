package se.chimps.bitziness.core.endpoints.net.io.protocols.somerpc

import java.nio.ByteOrder
import java.util.UUID

import akka.actor.ActorRef
import akka.io.Tcp.{CommandFailed, Write, WriteFile}
import akka.pattern.PipeToSupport
import akka.util.{ByteIterator, ByteString}
import se.chimps.bitziness.core.endpoints.net.io.Common.ConnectionBase

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.hashing.MurmurHash3

object Common {
  case class Error(sender:ActorRef, message:String, exception:Option[Throwable] = None)
  case class RpcRequest(method:Int, data:Seq[ByteString])
  case class RpcResponse(data:Seq[ByteString])
  case class NewConnection(connection:ActorRef)

  object Methods {
    val REPLY = 0
    val ERROR = -1
  }

  trait ByteStringHelpers { bytes:ConnectionBase =>
    implicit val byteOrder = ByteOrder.nativeOrder()

    def encoder:Option[PartialFunction[ByteString, ByteString]]
    def parseResult(method:Int, uuid:UUID, blobs:Seq[ByteString]):Unit

    val passThrough:PartialFunction[ByteString, ByteString] = {
      case bytes => bytes
    }

    def parse(bytes:ByteString):ByteString = {
      val it = bytes.iterator
      val method = it.getInt
      val uuidLen = it.getInt

      val uuidBytes = Array.ofDim[Byte](uuidLen)
      it.getBytes(uuidBytes)
      val uuid = UUID.fromString(new String(uuidBytes, "utf-8"))

      val blobCount = it.getInt
      val blobs:Seq[ByteString] = readBlobs(it, blobCount)

      parseResult(method, uuid, blobs)

      it.toByteString
    }

    def readBlobs(it:ByteIterator, blobCount:Int):Seq[ByteString] = {
      if (blobCount > 0) {
        val blobLen = it.getInt
        val blobCrc = it.getInt
        val blob:ByteString = it.take(blobLen).toByteString

        require(MurmurHash3.orderedHash(blob) == blobCrc, s"Invalid crc on blob #$blobCount!")

        Seq(blob) ++ readBlobs(it, blobCount - 1)
      } else {
        Seq()
      }
    }

    def write(method:Int, uuid:UUID, data:Seq[ByteString]):Unit = {
      val builder = ByteString.newBuilder

      val uuidAsBytes = uuid.toString.getBytes("utf-8")
      val uuidLen = uuidAsBytes.length

      builder.putInt(method)
      builder.putInt(uuidLen)
      builder.putBytes(uuidAsBytes)
      builder.putInt(data.size)

      data.foreach({ bytes =>
        builder.putInt(bytes.length)
        builder.putInt(MurmurHash3.orderedHash(bytes))
        builder.append(bytes)
      })

      val op = encoder.getOrElse(passThrough)

      write(op(builder.result()))
    }
  }

  trait PromiseStore {
    var promises = Map[UUID, Promise[RpcResponse]]()

    def add(uuid:UUID):Future[RpcResponse] = {
      val promise = Promise[RpcResponse]()
      promises = promises ++ Map(uuid -> promise)
      promise.future
    }

    def fulfill(uuid:UUID, resp:RpcResponse):Unit = {
      promises.get(uuid).foreach(_.success(resp))
      promises = promises - uuid
    }
  }

  trait RpcHelpers extends ByteStringHelpers with PromiseStore with PipeToSupport { self:ConnectionBase =>
    import context.dispatcher
    def service:ActorRef
    val encoder:Option[PartialFunction[ByteString, ByteString]] = None
    val decoder:Option[PartialFunction[ByteString, ByteString]] = None

    var buffer:ByteString = ByteString.empty

    def handleBytes(data:ByteString):Unit = {
      buffer = parse(buffer.concat(data))
    }

    override def parseResult(method: Int, uuid: UUID, blobs: Seq[ByteString]): Unit = {
      // are we blocking here?
      if (method == Methods.REPLY) {
        fulfill(uuid, RpcResponse(blobs))
      } else {
        handleRequest((method, uuid, blobs)).foreach(rep => reply(rep._1, rep._2))
      }
    }

    def request(method:Int, data:Seq[ByteString]):Future[RpcResponse] = {
      val uuid = UUID.randomUUID()
      val future = add(uuid)

      write(method, uuid, data)

      future
    }

    def reply(uuid:UUID, data:Seq[ByteString]):Unit = {
      write(Methods.REPLY, uuid, data)
    }

    def handleRequest:((Int, UUID, Seq[ByteString]))=>Future[(UUID, Seq[ByteString])]

    // ConnectionBase implementations.
    override def onData(data: ByteString): Unit = {
      val op = decoder.getOrElse(passThrough)
      handleBytes(op(data))
    }

    override def onClose(): Unit = context stop this.self

    override def onCommandFailed(cmd: CommandFailed): Unit = {
      val msg = cmd.cmd match {
        case w:Write => "An attempt to write data failed."
        case f:WriteFile => "An attempt to write a file failed."
      }

      service ! Error(this.self, msg)
    }

    override def otherConnectionLogic: Receive = {
      case RpcRequest(method, data) => {
        val queree = sender()
        request(method, data).pipeTo(queree)
      }
    }
  }
}
