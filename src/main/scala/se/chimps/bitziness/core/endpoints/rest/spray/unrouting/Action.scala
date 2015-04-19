package se.chimps.bitziness.core.endpoints.rest.spray.unrouting

import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Model.{Response, Request}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Action {
  def apply(func:(Request)=>Response):Action = new Action {
    override def apply(req:Request):Future[Response] = Future(func(req))
  }

  def apply(func:()=>Response):Action = new Action {
    override def apply(req:Request):Future[Response] = Future(func())
  }

  def async(func:(Request)=>Future[Response]):Action = new Action {
    override def apply(req: Request): Future[Response] = func(req)
  }

  def async(func:()=>Future[Response]):Action = new Action {
    override def apply(req: Request): Future[Response] = func()
  }
}

trait Action extends (Request => Future[Response]) {
  def apply() = this
}