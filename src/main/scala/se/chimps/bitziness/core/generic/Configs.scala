package se.chimps.bitziness.core.generic

import com.typesafe.config.{Config, ConfigFactory}
import scala.collection.convert.decorateAsScala._
import scala.util.Properties

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
      Properties.envOrNone(key).map(_.toInt)
    }
  }

  def asBoolean(key:String):Option[Boolean] = {
    if (config.hasPath(key)) {
      Some(config.getBoolean(key))
    } else {
      Properties.envOrNone(key).map(_.toBoolean)
    }
  }

  def asString(key:String):Option[String] = {
    if (config.hasPath(key)) {
      Some(config.getString(key))
    } else {
      Properties.envOrNone(key)
    }
  }

  def asIntList(key:String):List[Int] = {
    if (config.hasPath(key)) {
      config.getIntList(key).asScala.map(i => i.toInt).toList
    } else {
      List()
    }
  }

  def asBooleanList(key:String):List[Boolean] = {
    if (config.hasPath(key)) {
      config.getBooleanList(key).asScala.map(b => b.booleanValue()).toList
    } else {
      List()
    }
  }

  def asStringList(key:String):List[String] = {
    if (config.hasPath(key)) {
      config.getStringList(key).asScala.toList
    } else {
      List()
    }
  }

  def subConfig(key:String):Config = config.getConfig(key)
}
