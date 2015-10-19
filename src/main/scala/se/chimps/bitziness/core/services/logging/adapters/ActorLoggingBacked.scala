package se.chimps.bitziness.core.services.logging.adapters

import akka.actor.{Actor, ActorLogging}
import se.chimps.bitziness.core.services.logging.{LoggingService, Adapter}

class ActorLoggingBacked extends LoggingService with ActorLogging { self:Actor =>

  override def info(sender:String, message:String, meta:Map[String, String]):Unit = {
    log.mdc ++ meta.map(m => (m._1, m._2))

    log.info(fakeSender(shortSender(sender), message))
  }

  override def debug(sender:String, message:String, meta:Map[String, String]):Unit = {
    log.mdc ++ meta.map(m => (m._1, m._2))

    log.debug(fakeSender(shortSender(sender), message))
  }

  override def error(sender:String, message:String, meta:Map[String, String], error:Option[Throwable]):Unit = {
    log.mdc ++ meta.map(m => (m._1, m._2))

    if (error.isDefined) {
      log.error(error.get, fakeSender(shortSender(sender), message))
    } else {
      log.error(fakeSender(shortSender(sender), message))
    }
  }

  override def warn(sender:String, message:String, meta:Map[String, String], error:Option[Throwable]):Unit = {
    log.mdc ++ meta.map(m => (m._1, m._2))

    if (error.isDefined) {
      log.error(error.get, fakeSender(shortSender(sender), message))
    } else {
      log.error(fakeSender(shortSender(sender), message))
    }
  }

  override def unknown(level:String, sender:String, message:String, meta:Map[String, String], error:Option[Throwable]):Unit = {}

  def shortSender(sender:String):String = {
    sender.split("\\.").reverse.head
  }

  def fakeSender(sender:String, message:String):String = {
    s"[$sender] $message"
  }
}