package se.chimps.bitziness.core.generic.codecs

import akka.util.ByteString
import com.google.protobuf
import se.chimps.bitziness.core.generic.serializers.ProtobufSerializer

import scala.reflect.ClassTag

/**
	* Created by meduzz on 18/08/16.
	*/
class ProtobufDecoder[T >: protobuf.Message](implicit tag:ClassTag[T]) extends Decoder[ByteString, T] with ProtobufSerializer {
	override def decode(in:ByteString):T = deserialize(in.toArray)
}
