package se.chimps.bitziness.core.generic

import scala.reflect.ClassTag

/**
 * Trait that can can look up if a candidate has a certain feature.
 */
trait HasFeature {

  /**
   * Does a isInstanceOF[T] check on candidate.
   * @param candidate the instance that might be an instance of T.
   * @tparam T the type or feature to look for.
   * @return returns TorF
   */
  def hasFeature[T : ClassTag](candidate:Any):Boolean = {
    val clazz = implicitly[ClassTag[T]].runtimeClass
    clazz.isInstance(candidate)
  }
}
