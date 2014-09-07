package se.chimps.bitziness.core.endpoints.rest.spray.unrouting

import se.chimps.bitziness.core.endpoints.rest.spray.unrouting.Model.{Response, Request}

import scala.util.matching.Regex

object Action {
  def apply(func:(Request)=>Response):Action = new Action {
    override def apply(req:Request):Response = func(req)
  }

  def apply(func:()=>Response):Action = new Action {
    override def apply(req:Request):Response = func()
  }
}

trait Action extends (Request => Response) {
  def apply() = this
}
