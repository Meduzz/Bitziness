package se.chimps.bitziness.core.generic

import akka.actor.Actor.Receive

/**
 * Created by meduzz on 22/08/14.
 */
trait ReceiveChain {
  private var pfs = Seq[Receive]()

  private[core] def registerReceive(method:Receive):Unit = {
    pfs = pfs ++ Seq[Receive](method)
  }

  protected lazy val receives = pfs.reduce((a:Receive, b:Receive) => a.orElse(b))
}
