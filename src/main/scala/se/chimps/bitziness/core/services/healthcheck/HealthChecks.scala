package se.chimps.bitziness.core.services.healthcheck

/**
 *
 */
object HealthChecks {
  var hcs:Map[String, Function0[Boolean]] = Map()

  def register(name:String, hc:()=>Boolean):Unit = {
    hcs = hcs ++ Map(name -> hc)
  }

  def execute():Map[String, Boolean] = hcs.map(hc => hc._1 -> hc._2())
}
