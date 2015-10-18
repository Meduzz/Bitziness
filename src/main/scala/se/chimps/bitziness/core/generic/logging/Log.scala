package se.chimps.bitziness.core.generic.logging

import akka.actor.{Stash, Actor}
import se.chimps.bitziness.core.generic.{Event, Events}

trait Log extends Events with LogHelpers { myself:Actor =>
  var buffer = List[LogEvent]()

  def log(sender:String, level:String, message:String, meta:Map[String, String], throwable:Option[Throwable]):Unit = {
    if (eventStream != null) {
      publish(LogEvent(sender, level, message, meta, throwable))
    }
  }
}

case class LogEvent(sender:String, level:String, message:String, meta:Map[String, String], throwable:Option[Throwable]) extends Event

trait LogHelpers {
  val callingClass = getClass.getName

  val INFO = "INFO"
  val DEBUG = "DEBUG"
  val ERROR = "ERROR"
  val WARN = "WARN"

  def log(sender:String, level:String, message:String, meta:Map[String, String], throwable:Option[Throwable]):Unit

  def info(message:String, meta:Map[String, String] = Map()) = {
    log(callingClass, INFO, message, meta, None)
  }

  def debug(message:String, meta:Map[String, String] = Map()) = {
    log(callingClass, DEBUG, message, meta, None)
  }

  def error(message:String, meta:Map[String, String] = Map(), error:Option[Throwable] = None) = {
    log(callingClass, ERROR, message, meta, error)
  }

  def warn(message:String, meta:Map[String, String] = Map(), error:Option[Throwable] = None) = {
    log(callingClass, WARN, message, meta, error)
  }
}