package example

import se.chimps.bitziness.core.project.AbstractProject

object TestProject extends AbstractProject {
  override def initialize(args:Array[String]):Unit = {
    registerService(classOf[TestService])
  }
}
