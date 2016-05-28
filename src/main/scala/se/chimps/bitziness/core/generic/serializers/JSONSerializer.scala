package se.chimps.bitziness.core.generic.serializers

trait JSONSerializer {
	import org.json4s._
	import org.json4s.native.JsonMethods._
	import org.json4s.native.Serialization.write
	implicit private val formats = DefaultFormats

	def fromJSON[T](json:String)(implicit manifest:Manifest[T]):T = parse(json).extract[T]
	def toJSON(instance:AnyRef):String =  write(instance)
}
