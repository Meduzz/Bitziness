package se.chimps.bitziness.core.generic

import akka.actor.ActorSystem
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

  override protected def afterAll():Unit = {
    serializer.shutdown()
  }
}

class Serializer extends ObjectSerializer with JSONSerializer {
  protected val system = ActorSystem()

  def shutdown() = system.shutdown()
}

case class Simple(name:String)
