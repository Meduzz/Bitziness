package se.chimps.bitziness.core.generic

/**
 * Something to point your rescue at.
 */
trait ErrorMapping[T] {
  def errorMapping:PartialFunction[Throwable, T]
}
