package se.chimps.bitziness.core.generic.serializers

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

trait ProtobufSerializer {
	import java.lang.reflect.Method

	import com.google.protobuf.Message

	def serialize[T <: Message](instance:T):Array[Byte] = instance.toByteArray
	def deserialize[T <: Message](bytes:Array[Byte])(implicit tag:ClassTag[T]):T = findMethod(tag).invoke(tag.runtimeClass, bytes).asInstanceOf[T]

	private def findMethod[T <: Message](tag:ClassTag[T]):Method = {
		Try {
			tag.runtimeClass.getDeclaredMethod("parseFrom", classOf[Array[Byte]])
		} match {
			case Success(method) => method
			case Failure(e) => throw e
		}
	}
}
