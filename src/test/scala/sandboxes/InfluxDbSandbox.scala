package sandboxes

import akka.actor.{ActorSystem, Props}
import se.chimps.bitziness.core.generic.Init
import se.chimps.bitziness.core.generic.metrics.{BooleanMetric, DecimalMetric, LongMetric, StringMetric}
import se.chimps.bitziness.core.services.metrics.adapters.{InfluxDbMetricsService, InfluxSettings}

object InfluxDbSandbox extends App {
  implicit lazy val system = ActorSystem("InfluxDbSandbox")
  val settings = InfluxSettings("192.168.235.20", 8086, "test", Some("tests"), Some("secret"), false, 10)

  val service = system.actorOf(Props(classOf[InfluxDbMetricsService], settings, "sandbox"))
  service ! Init

  service ! StringMetric("blue", "test", "color", None, Map())
  service ! BooleanMetric(true, "test", "boolean", None, Map())
  service ! DecimalMetric(BigDecimal("22.53"), "test", "decimal", None, Map())
  (1 to 7).foreach(i => service ! LongMetric(i, "test", "test1", None, Map()))

  system.terminate()
}
