package se.chimps.bitziness.core.endpoints.persistence.activate

import net.fwbrasil.activate.ActivateContext
import net.fwbrasil.radon.transaction.Transaction
import se.chimps.bitziness.core.generic.Configs

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait ActivateFactory extends Configs {

  def dbContext:ActivateContext

  def transaction[T](op:()=>T):T = {
    dbContext.transactional {
      op()
    }
  }

  def tryTransaction[T](op:()=>T):Try[T] = {
    val tx = new Transaction()(dbContext)
    val t = Try {
      dbContext.transactional(tx) {
        op()
      }
    }

    t match {
      case s:Success[T] => tx.commit(); s
      case f:Failure[T] => tx.rollback(); f
    }
  }

  def asyncTransaction[T](op:()=>T):Future[T] = {
    dbContext.asyncTransactional {
      op()
    }
  }
}
