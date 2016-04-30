package se.chimps.bitziness.core.services.logging

import se.chimps.bitziness.core.Service
import se.chimps.bitziness.core.generic.Events
import se.chimps.bitziness.core.generic.logging.LogEvent

class LoggingService(val delegate:LoggingDelegate) extends Service with Events {

  override def handle:Receive = {
    case LogEvent(sender, level, message, meta, throwable) => level match {
      case "INFO" => delegate.info(sender, message, meta)
      case "ERROR" => delegate.error(sender, message, meta, throwable)
      case "DEBUG" => delegate.debug(sender, message, meta)
      case "WARN" => delegate.warn(sender, message, meta, throwable)
      case _ => delegate.unknown(level, sender, message, meta, throwable)
    }
  }

  override def initialize():Unit = {
    internalEventsBuilder.subscribe(classOf[LogEvent])
  }
}

trait LoggingDelegate {
  def info(sender:String, message:String, meta:Map[String, String])

  def debug(sender:String, message:String, meta:Map[String, String])

  def error(sender:String, message:String, meta:Map[String, String], error:Option[Throwable])

  def warn(sender:String, message:String, meta:Map[String, String], error:Option[Throwable])

  def unknown(level:String, sender:String, message:String, meta:Map[String, String], error:Option[Throwable])
}
