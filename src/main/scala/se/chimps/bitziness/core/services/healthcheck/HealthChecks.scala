package se.chimps.bitziness.core.services.healthcheck

import scala.concurrent.{ExecutionContext, Future}

/**
 *
 */
object HealthChecks {
  var hcs:Map[String, Function0[Boolean]] = Map()

  def register(name:String, hc:()=>Boolean):Unit = {
    hcs = hcs ++ Map(name -> hc)
  }

  def execute()(implicit ec:ExecutionContext):Map[String, Future[Boolean]] = hcs.map(hc => hc._1 -> Future { hc._2() })
}
