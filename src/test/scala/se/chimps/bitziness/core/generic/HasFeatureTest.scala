package se.chimps.bitziness.core.generic

import org.scalatest.FunSuite

/**
 * HasFeature tests.
 */
class HasFeatureTest extends FunSuite {

  test("has feature happy case") {
    val obj = new Features

    assert(obj.hasFeature[String]("asdf"))
    assert(!obj.hasFeature[Int](null))
    assert(!obj.hasFeature[Int]("asdf"))
    assert(!obj.hasFeature[String](Array[Int]()))
  }
}

private class Features extends HasFeature {

}