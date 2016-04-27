package sandboxes

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import se.chimps.bitziness.core.generic.Init
import se.chimps.bitziness.core.generic.metrics.{BooleanMetric, DecimalMetric, LongMetric, StringMetric}
import se.chimps.bitziness.core.services.metrics.adapters.{RiemannMetricsService, RiemannSettings}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object RiemannSandbox extends App {
  val system = ActorSystem("RiemannSandbox")
  val settings = RiemannSettings("192.168.235.20", 5555, 5)

  val service = system.actorOf(Props(classOf[RiemannMetricsService], settings, "sandbox"))
  service ! Init

  service ! StringMetric("blue", "test", "color", None, Map())
  service ! BooleanMetric(true, "test", "boolean", None, Map())
  service ! DecimalMetric(BigDecimal("22.53"), "test", "decimal", None, Map())
  (1 to 2).foreach(i => service ! LongMetric(i, "test", "test1", None, Map()))

  val g = Future(Thread.sleep(1500L))
  Await.ready(g, Duration(3L, TimeUnit.SECONDS))

  system.terminate()
}
