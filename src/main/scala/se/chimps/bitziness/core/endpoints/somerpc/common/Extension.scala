package se.chimps.bitziness.core.endpoints.somerpc.common

/**
 *
 */
trait Extension {
  type T
  type K

  def sum:Int
  def execute:PartialFunction[T, K]
  def t:Type
}

case class Type(label:String)

object Type {
  val BINARY = Type("BINARY")
}