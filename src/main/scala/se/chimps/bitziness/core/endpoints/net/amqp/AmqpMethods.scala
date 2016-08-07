package se.chimps.bitziness.core.endpoints.net.amqp

import akka.actor.ActorRef
import com.rabbitmq.client.AMQP.BasicProperties
import io.github.drexin.akka.amqp.AMQP.{Publish, Subscribe}

/**
 * Remethodify the AMQP methods
 */
trait AmqpMethods { endpoint:AmqpEndpoint =>

  protected def subscribe(queueName:String, autoack:Boolean = true) = {
    connection ! new Subscribe(queueName, autoack)
  }

  protected def publish[T](exchange:String, routingKey:String, body:T, mandatory:Boolean = false, immidiate:Boolean = false, props:Option[BasicProperties] = None)(implicit converter:(T)=>Array[Byte]) = {
    connection ! Publish(exchange, routingKey, converter(body), mandatory, immidiate, props)
  }

  // TODO implement the rest.
}
