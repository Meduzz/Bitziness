package se.chimps.bitziness.core.service

import akka.actor.Actor
import se.chimps.bitziness.core.generic.{Init, HasFeature}

/**
 * The base class for all services.
 * This is where most of your business logic will reside or be knit together.
 */
abstract class AbstractService extends Service {

}

trait Service extends Actor with HasFeature {
  private var pfs = Seq[Receive](init)

  def handle:Receive

  private def init:Receive = {
    case Init => initialize()
  }

  def registerReceive(method:Receive):Unit = {
    pfs = pfs ++ Seq[Receive](method)
  }

  private lazy val receives = (pfs ++ Seq(handle)) reduce((a:Receive, b:Receive) => a.orElse(b))
  override def receive:Receive = receives

  def initialize():Unit = {}
}


