package se.chimps.bitziness.core.generic.metrics

import akka.actor.Actor
import se.chimps.bitziness.core.generic.{Event, Events}

trait Metrics extends Events { myself:Actor =>
  def incr(counter:String, value:Long = 1L) = {
    publish(Counter(counter, value))
  }

  def decr(counter:String, value:Long = -1L) = {
    publish(Counter(counter, value))
  }

  def gauge(name:String, value:Long) = {
    publish(Gauge(name, value))
  }

  def histogram(name:String, value:BigDecimal) = {
    publish(Histogram(name, value))
  }

  def meter(name:String, value:BigDecimal, timestamp:Long) = {
    publish(Meter(name, value, timestamp))
  }

  def timer(name:String, value:Long) = {
    publish(Timer(name, value))
  }

  def timed[T, K](name:String)(op:(T)=>K):(T)=>K = (input:T) => {
    val start = System.nanoTime()
    val output = op(input)
    val end = System.nanoTime()

    timer(name, end-start)

    output
  }
}

trait Metric[T] extends Event {
  def value:T
  def name:String
}

case class Counter(name:String, value:Long) extends Metric[Long]
case class Gauge(name:String, value:Long) extends Metric[Long]
case class Histogram(name:String, value:BigDecimal) extends Metric[BigDecimal]
case class Meter(name:String, value:BigDecimal, timestamp:Long) extends Metric[BigDecimal]
case class Timer(name:String, value:Long) extends Metric[Long]