package se.chimps.bitziness.core.generic

import com.typesafe.config.{Config, ConfigFactory}
import scala.collection.convert.decorateAsScala._

object Configs {
  implicit def defaultHelper[T](option: Option[T]):Default[T] = {
    new Default[T](option)
  }

  class Default[T](val value:Option[T]) {
    def default(t:T):T = {
      value.getOrElse(t)
    }
  }
}

trait Configs {
  def fileName:Option[String] = None

  lazy val config = fileName match {
    case Some(file) => ConfigFactory.load(file)
    case None => ConfigFactory.load()
  }

  def asInt(key:String):Option[Int] = {
    if (config.hasPath(key)) {
      Some(config.getInt(key))
    } else {
      None
    }
  }

  def asBoolean(key:String):Option[Boolean] = {
    if (config.hasPath(key)) {
      Some(config.getBoolean(key))
    } else {
      None
    }
  }

  def asString(key:String):Option[String] = {
    if (config.hasPath(key)) {
      Some(config.getString(key))
    } else {
      None
    }
  }

  def asIntList(key:String):Option[List[Int]] = {
    if (config.hasPath(key)) {
      Some(config.getIntList(key).asScala.map(i => i.toInt).toList) // jeez!
    } else {
      None
    }
  }

  def asBooleanList(key:String):Option[List[Boolean]] = {
    if (config.hasPath(key)) {
      Some(config.getBooleanList(key).asScala.map(b => b.booleanValue()).toList)
    } else {
      None
    }
  }

  def asStringList(key:String):Option[List[String]] = {
    if (config.hasPath(key)) {
      Some(config.getStringList(key).asScala.toList)
    } else {
      None
    }
  }

  def subConfig(key:String):Config = config.getConfig(key)
}
