package se.chimps.bitziness.core.generic

import scala.reflect.ClassTag

/**
 * Serialization traits etc.
 */
object Serializers {

  trait ObjectSerializer {
    import akka.actor.ActorSystem
    import akka.serialization.SerializationExtension

    protected def system:ActorSystem
    lazy protected val extension = SerializationExtension(system)

    def serialize(instance: AnyRef):Array[Byte] = extension.serialize(instance).get // let it crash.
    def deserialize[T](bytes:Array[Byte])(implicit evidence:ClassTag[T]):T = extension.serializerFor(evidence.runtimeClass).fromBinary(bytes).asInstanceOf[T]
  }

  trait JSONSerializer {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    import org.json4s.native.Serialization.write
    implicit private val formats = DefaultFormats

    def fromJSON[T](json:String)(implicit manifest:Manifest[T]):T = parse(json).extract[T]
    def toJSON(instance:AnyRef):String =  write(instance)
  }

}
