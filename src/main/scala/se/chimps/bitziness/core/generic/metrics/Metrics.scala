package se.chimps.bitziness.core.generic.metrics

import akka.actor.Actor
import se.chimps.bitziness.core.generic.{Naming, Event, Events}

import scala.concurrent.{Promise, Future}
import scala.concurrent.ExecutionContext.Implicits.global

trait Metrics extends Events { myself:Actor with Naming =>

  def longMetric(metricName:String, metric:Long, state:Option[String] = None, metadata:Map[String, String] = Map()) = {
    publish(LongMetric(metric, name(), metricName, state, metadata))
  }

  def decimalMetric(metricName:String, metric:BigDecimal, state:Option[String] = None, metadata:Map[String, String] = Map()) = {
    publish(DecimalMetric(metric, name(), metricName, state, metadata))
  }

  def booleanMetric(metricName:String, metric:Boolean, state:Option[String] = None, metadata:Map[String, String] = Map()) = {
    publish(BooleanMetric(metric, name(), metricName, state, metadata))
  }

  def stringMetric(metricName:String, metric:String, state:Option[String] = None, metadata:Map[String, String] = Map()) = {
    publish(StringMetric(metric, name(), metricName, state, metadata))
  }

  def timed[T, K](name:String)(op:(T)=>K):(T)=>K = (input:T) => {
    val start = System.nanoTime()
    val output = op(input)
    val end = System.nanoTime()

    longMetric(name, end-start)

    output
  }

  def timedFuture[T, K](name:String)(op:(T)=>Future[K]):(T)=>Future[K] = (input:T) => {
    val start= System.nanoTime()
    val promise = Promise[K]()

    op(input).foreach(k => {
      val end = System.nanoTime()
      promise.success(k)
      longMetric(name, end - start)
    })

    promise.future
  }
}

trait Metric[T] extends Event {
  def value:T
  def service:String
  def name:String
  def state:Option[String]
  def metadata:Map[String, String]
}

case class LongMetric(value:Long, service:String, name:String, state:Option[String], metadata:Map[String, String]) extends Metric[Long]
case class DecimalMetric(value:BigDecimal, service:String, name:String, state:Option[String], metadata:Map[String, String]) extends Metric[BigDecimal]
case class BooleanMetric(value:Boolean, service:String, name:String, state:Option[String], metadata:Map[String, String]) extends Metric[Boolean]
case class StringMetric(value:String, service:String, name:String, state:Option[String], metadata:Map[String, String]) extends Metric[String]
