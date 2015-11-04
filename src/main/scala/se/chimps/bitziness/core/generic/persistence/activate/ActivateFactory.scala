package se.chimps.bitziness.core.generic.persistence.activate

import net.fwbrasil.activate.ActivateContext
import net.fwbrasil.radon.transaction.Transaction
import se.chimps.bitziness.core.generic.Configs

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait ActivateFactory extends Configs {

  def context:ActivateContext

  def withTransaction[T](op:()=>T):T = {
    context.transactional {
      op()
    }
  }

  def withTransaction[T](op:()=>T):Try[T] = {
    val tx = new Transaction()
    val t = Try {
      context.transactional(tx) {
        op()
      }
    }

    t match {
      case s:Success => tx.commit(); s
      case f:Failure => tx.rollback(); f
    }
  }

  def withAsyncTransaction[T](op:()=>T):Future[T] = {
    context.asyncTransactional {
      op()
    }
  }
}
