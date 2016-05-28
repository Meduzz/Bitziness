package se.chimps.bitziness.core.generic.serializers

import scala.reflect.ClassTag

trait ObjectSerializer {
	import akka.actor.ActorSystem
	import akka.serialization.SerializationExtension

	protected def system:ActorSystem
	lazy protected val extension = SerializationExtension(system)

	def serialize(instance: AnyRef):Array[Byte] = extension.serialize(instance).get // let it crash.
	def deserialize[T](bytes:Array[Byte])(implicit evidence:ClassTag[T]):T = extension.serializerFor(evidence.runtimeClass).fromBinary(bytes).asInstanceOf[T]
}
