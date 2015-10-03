package se.chimps.bitziness.core.endpoints.somerpc.common

import java.io._

object Extensions {

  /**
   *
   */
  trait Binary extends Extension {
    type T = Array[Byte]
    type K = Array[Byte]

    override def extensionType: Type = Type.BINARY
  }

  /**
   *
   */
  trait Decoder extends Extension {
    override type K = Array[Byte]
    override type T = Any

    override def extensionType: Type = Type.DECODER
  }

  class JavaDecoder extends Decoder {
    override def execute: PartialFunction[Any, Array[Byte]] = {
      case t:Any => useJavaSerialization(t)
    }

    def useJavaSerialization(anyRef:Any):Array[Byte] = {
      val bos = new ByteArrayOutputStream
      val oos = new ObjectOutputStream(bos)

      oos.writeObject(anyRef.asInstanceOf[Serializable])
      bos.toByteArray
    }

    override def sum: Int = 10
  }

  /**
   *
   */
  trait Encoder extends Extension {
    override type T = Array[Byte]
    override type K = Any

    override def extensionType: Type = Type.ENCODER
  }

  class JavaEncoder extends Encoder {
    override def execute: PartialFunction[Array[Byte], Any] = {
      case data:Array[Byte] => useJavaDeserialization(data)
    }

    def useJavaDeserialization(bytes:Array[Byte]):Any = {
      val bis = new ByteArrayInputStream(bytes)
      val ois = new ObjectInputStream(bis)
      ois.readObject()
    }

    override def sum: Int = 20
  }
}