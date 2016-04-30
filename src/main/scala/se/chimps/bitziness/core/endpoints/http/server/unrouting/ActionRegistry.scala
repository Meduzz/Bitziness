package se.chimps.bitziness.core.endpoints.http.server.unrouting

/**
 *
 */
trait ActionRegistry {

  private[unrouting] var actions:Map[String, List[ActionDefinition]] = Map()

  def registerController(controller:Controller):Unit = {
    controller.definitions.groupBy(_.method).foreach(kv => {
      val (method, acts) = kv
      actions = actions ++ Map(method -> (actions.getOrElse(method, List()) ++ acts))
    })
  }

  def registerDefinition(definition: ActionDefinition):Unit = {
		actions = actions ++ Map(definition.method -> (actions.getOrElse(definition.method, List()) ++ List(definition)))
	}
}
