package se.chimps.bitziness.core.generic

import org.scalatest.FunSuite

class ConfigsTest extends FunSuite {
  import Configs._
  val subject = new ConfigSubject

  test("values in the config are fetched from config") {
    val value = subject.asString("special.value.goes.here").default("nopes")

    assert(value.equals("Yes"))
  }

  test("values not in the config are using defaults") {
    val value = subject.asInt("key.that.does.not.exists").default(100)

    assert(value.equals(100))
  }
}

class ConfigSubject extends Configs {
  override val fileName:Option[String] = Some("special.conf")
}