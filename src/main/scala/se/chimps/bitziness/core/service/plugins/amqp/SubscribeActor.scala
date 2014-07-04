package se.chimps.bitziness.core.service.plugins.amqp

import akka.actor.{ActorRef, Actor}
import io.github.drexin.akka.amqp.AMQP.{Subscribe, Delivery}
import akka.agent.Agent

/**
 * Created by meduzz on 05/07/14.
 */
class SubscribeActor extends Actor {
  import scala.concurrent.ExecutionContext.Implicits.global

  val handlerAgent:Agent[(Delivery)=>Unit] = Agent.apply(dl => println(dl))

  override def receive:Receive = {
    case s:Delivery => handlerAgent.apply()(s)
    case s:SubscribeCommand => {
      handlerAgent.send(s.handler)
      s.connection ! s.subscribe
    }
  }
}

case class SubscribeCommand(connection:ActorRef, subscribe:Subscribe, handler:(Delivery)=>Unit)
