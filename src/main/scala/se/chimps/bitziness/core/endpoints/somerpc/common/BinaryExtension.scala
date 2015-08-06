package se.chimps.bitziness.core.endpoints.somerpc.common

/**
 *
 */
trait BinaryExtension extends Extension {
  type T = Array[Byte]
  type K = Array[Byte]

  override def t: Type = Type.BINARY
}
