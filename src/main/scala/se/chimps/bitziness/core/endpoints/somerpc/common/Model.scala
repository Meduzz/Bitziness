package se.chimps.bitziness.core.endpoints.somerpc.common

import java.util.UUID

/**
 *
 */
object Model {
  case class Transmit(method:Int, uuid:UUID, data:Seq[Array[Byte]])
}
