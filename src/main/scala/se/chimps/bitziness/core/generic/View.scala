package se.chimps.bitziness.core.generic

/**
 *
 */
trait View {
  def render():Array[Byte]
  def contentType:String
  def charset:String
}