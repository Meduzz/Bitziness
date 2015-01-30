package se.chimps.bitziness.core.generic

import akka.actor.ActorSystem
import org.json4s.{ShortTypeHints, DefaultFormats}
import org.json4s.native.Serialization
import org.scalatest.{BeforeAndAfterAll,FunSuite}
import se.chimps.bitziness.core.generic.Serializers.{JSONSerializer, ObjectSerializer}

/**
 * Tests of serializers.
 */
class SerializersTest extends FunSuite with BeforeAndAfterAll {

  val serializer = new Serializer

  test("object can go into bytes and back") {
    val name = "åö?!*≥" // funny name huh?
    val subject = Simple(name)

    val bytes = serializer.serialize(subject)
    assert(bytes.length > 0, "Bytes were not more than 0 in length.")

    val reborn = serializer.deserialize[Simple](bytes)
    assert(reborn.name.equals(name), "the reborns name did not match the original name.")
    assert(reborn.equals(subject), "the reborn does not equal the subject.")
  }

  test("object can go into json and back") {
    val name = "åäö?!*≥"
    val subject = Simple(name)

    val json = serializer.toJSON(subject)
    assert(json != null, "the json string was null...")
    assert(!json.isEmpty, "the json string was empty.")
    assert(json.contains(name), "the json string does not contain name.")

    val reborn = serializer.fromJSON[Simple](json)
    assert(reborn.name.equals(name), "the reborns name did not match the original name.")
    assert(reborn.equals(subject), "the reborn did not equal the subject.")
  }

  test("list of objects can go into json and back") {
    val subject = List(Simple("1"), Simple("2"))

    val json = serializer.toJSON(subject)
    assert(json != null, "the json string was null")
    assert(!json.isEmpty, "the json string was empty")

    val reborn = serializer.fromJSON[List[Simple]](json)
    assert(reborn.size == 2, "the size of the reborn are incorrect.")
    assert(reborn.equals(subject), "the reborn does not equal the subject")
  }

  test("list of objects can go into bytes and back") {
    val subject = List(Simple("1"), Simple("2"))

    val bytes = serializer.serialize(subject)
    assert(bytes.length > 0, "bytes were not more than 0 in length.")
    println(bytes.length)
    bytes.foreach(b => print(b.toChar))
    println()

    val reborn = serializer.deserialize[List[Simple]](bytes)
    assert(reborn.size == subject.size, "reborn had different size than subject.")
    assert(reborn.equals(subject), "reborn was not equal to the subject.")
  }

  override protected def afterAll():Unit = {
    serializer.shutdown()
  }
}

class Serializer extends ObjectSerializer with JSONSerializer {
  protected val system = ActorSystem()

  def shutdown() = system.shutdown()
}

case class Simple(name:String)
