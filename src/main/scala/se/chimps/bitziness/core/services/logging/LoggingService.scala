package se.chimps.bitziness.core.services.logging

import se.chimps.bitziness.core.Service
import se.chimps.bitziness.core.generic.Events
import se.chimps.bitziness.core.generic.logging.LogEvent

trait LoggingService extends Service with Events with Adapter {

  override def handle:Receive = {
    case LogEvent(sender, level, message, meta, throwable) => level match {
      case "INFO" => info(sender, message, meta)
      case "ERROR" => error(sender, message, meta, throwable)
      case "DEBUG" => debug(sender, message, meta)
      case "WARN" => warn(sender, message, meta, throwable)
      case _ => unknown(level, sender, message, meta, throwable)
    }
  }

  override def initialize():Unit = {
    this.internalEventsBuilder.subscribe(classOf[LogEvent])
  }
}

trait Adapter {
  def info(sender:String, message:String, meta:Map[String, String]) = {}

  def debug(sender:String, message:String, meta:Map[String, String]) = {}

  def error(sender:String, message:String, meta:Map[String, String], error:Option[Throwable]) = {}

  def warn(sender:String, message:String, meta:Map[String, String], error:Option[Throwable]) = {}

  def unknown(level:String, sender:String, message:String, meta:Map[String, String], error:Option[Throwable]) = {}
}
