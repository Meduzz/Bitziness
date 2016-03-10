package se.chimps.bitziness.core.generic.view

import org.fusesource.scalate._
import se.chimps.bitziness.core.generic.View

/**
 * A view built on the excellent Scalate framework.
 */
object Scalate {
  implicit val templateEngine = new TemplateEngine()

  def apply(template:String, model:Map[String, Any]):Scalate = {
    new Scalate(template, model)
  }

  def apply(template:String):Scalate = {
    new Scalate(template, Map())
  }
}

class Scalate(val template:String, val model:Map[String, Any])(implicit val engine:TemplateEngine) extends View {

  override def render(): Array[Byte] = {
    engine.layout(template, model).getBytes("utf-8")
  }

  override def contentType: String = "text/html"

  override def charset:String = "utf-8"
}