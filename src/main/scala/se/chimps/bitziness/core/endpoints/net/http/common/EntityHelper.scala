package se.chimps.bitziness.core.endpoints.net.http.common

import akka.http.scaladsl.model.HttpEntity
import akka.stream.Materializer
import akka.util.ByteString
import com.google.protobuf.Message
import se.chimps.bitziness.core.generic.serializers.{JSONSerializer, ProtobufSerializer}

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

/**
	* These helpers will drain the entity.dataBytes stream into a ByteString.
	* If you want to boot a full blown akka-stream pipe to parse a json string, be my guest.
	*/
trait JsonEntityHelper {

	implicit def jsonExplicits(entity:HttpEntity)(implicit ec:ExecutionContext, mat:Materializer):JsonHelper = new JsonHelper(entity)

}

/**
	* These helpers will drain the entity.dataBytes stream into a ByteString.
	* If you want to boot a full blown akka-stream pipe to parse a json string, be my guest.
	*/
trait ProtoEntityHelper {

	implicit def jsonExplicits(entity:HttpEntity)(implicit ec:ExecutionContext, mat:Materializer):JsonHelper = new JsonHelper(entity)

}

trait EntityHelper {

	implicit def mat:Materializer
	implicit def ec:ExecutionContext

	def entity:HttpEntity

	def asString():Future[String] = {
		entity.dataBytes
			.runFold(ByteString.empty)((a,b) => a.concat(b))
			.map(_.utf8String)
	}

	def asBytes():Future[Array[Byte]] = {
		entity.dataBytes
			.runFold(ByteString.empty)((a,b) => a.concat(b))
			.map(_.toArray)
	}

}

class JsonHelper(val entity: HttpEntity)(implicit val ec:ExecutionContext, val mat:Materializer) extends JSONSerializer with EntityHelper {

	def asJson[T]()(implicit tag:Manifest[T]):Future[T] = {
		entity.dataBytes
			.runFold(ByteString.empty)((a,b) => a.concat(b))
			.map(_.utf8String)
			.map(fromJSON[T])
	}

}

class ProtoHelper(val entity: HttpEntity)(implicit val ec:ExecutionContext, val mat:Materializer) extends ProtobufSerializer with EntityHelper {

	def asProtobuf[T >: Message]()(implicit tag:ClassTag[T]):Future[T] = {
		entity.dataBytes
			.runFold(ByteString.empty)((a,b) => a.concat(b))
			.map(_.toArray)
			.map(deserialize)
	}

}
