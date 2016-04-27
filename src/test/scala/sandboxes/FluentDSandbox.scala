package sandboxes

import java.util.concurrent.TimeUnit

import akka.actor.{Props, ActorSystem}
import akka.util.Timeout
import se.chimps.bitziness.core.generic.Init
import se.chimps.bitziness.core.generic.logging.LogEvent
import se.chimps.bitziness.core.services.logging.adapters.{FluentDLoggingService, FluentDSettings}

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

object FluentDSandbox extends App {
  val settings = FluentDSettings("192.168.235.20", 7070, false)
  implicit lazy val system = ActorSystem("FluentDSandbox")
  implicit val timeout = Timeout(3L, TimeUnit.SECONDS)

  val service = system.actorOf(Props(classOf[FluentDLoggingService], settings, "localhost", "sandbox"))
  service ! Init
/*
  val f = Future(Thread.sleep(1500L))
  Await.ready(f, Duration(3L, TimeUnit.SECONDS))
*/

  service ! LogEvent("sandbox.FluentDSandbox", "INFO", "I can haz logz?", Map("key" -> "value"), None)
  service ! LogEvent("sandbox.FluentDSandbox", "DEBUG", "I can haz debug logz?", Map("key2" -> "value"), None)
  service ! LogEvent("sandbox.FluentDSandbox", "ERROR", "I can haz error logz?", Map("key1" -> "value"), Some(new RuntimeException("Bam!")))
  service ! LogEvent("sandbox.FluentDSandbox", "asdf", "I can haz unknown logz?", Map("key3" -> "value"), None)

  val g = Future(Thread.sleep(1500L))
  Await.ready(g, Duration(3L, TimeUnit.SECONDS))

  system.terminate()
}
