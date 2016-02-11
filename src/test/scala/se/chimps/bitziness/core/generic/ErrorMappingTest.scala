package se.chimps.bitziness.core.generic

import org.scalatest.FunSuite

class ErrorMappingTest extends FunSuite {
  val subject = new ErrorMapper

  test("Errors go straight through") {
    val error = new RuntimeException("Some random error.")
    val response = subject.errorMapping(error)

    assert(error.equals(response))
  }
}

class ErrorMapper extends ErrorMapping[Any] {
  override def errorMapping:PartialFunction[Throwable, Any] = {
    case e:Throwable => e
  }
}