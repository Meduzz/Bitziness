package se.chimps.bitziness.core.endpoints.rest.spray.unrouting

/**
 *
 */
case class Host(host:String, port:Int) {
  override def toString: String = s"${host}:${port}"
}
