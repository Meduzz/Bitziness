package se.chimps.bitziness.core.endpoints.somerpc.common

/**
 *
 */
// TODO rework extensions to different classes, like Encode/decode, binary and possible one more I cant think of atm.
trait Extension {
  type T
  type K

  def sum:Int
  def execute:PartialFunction[T, K]
  def extensionType:Type
}

case class Type(label:String)

object Type {
  val BINARY = Type("BINARY")
  val ENCODER = Type("ENCODER")
  val DECODER = Type("DECODER")
}