package se.chimps.bitziness.core.endpoints.amqp

import akka.actor.ActorRef
import com.rabbitmq.client.AMQP.BasicProperties
import io.github.drexin.akka.amqp.AMQP.{Publish, Subscribe}

/**
 * Remethodify the AMQP methods
 */
trait AmqpMethods {
  def ampqConnection:ActorRef

  protected def subscribe(queueName:String, autoack:Boolean = true) = {
    ampqConnection ! new Subscribe(queueName, autoack)
  }

  protected def publish[T](exchange:String, routingKey:String, body:T, mandatory:Boolean = false, immidiate:Boolean = false, props:Option[BasicProperties] = None)(implicit converter:(T)=>Array[Byte]) = {
    ampqConnection ! Publish(exchange, routingKey, converter(body), mandatory, immidiate, props)
  }

  // TODO implement the rest.
}
