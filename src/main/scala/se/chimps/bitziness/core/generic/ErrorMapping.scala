package se.chimps.bitziness.core.generic

/**
 * A trait that gives you a method to put all your error mapping.
 * Defautls to no mapping.
 */
trait ErrorMapping {
  def errorMapping:PartialFunction[Throwable, Any] = {
    case e:Throwable => throw e
  }
}
