package se.chimps.bitziness.core.endpoints.http.server.unrouting

/**
 *
 */
trait ActionRegistry {

  private[unrouting] var actions:Map[String, List[ActionDefinition]] = Map()

  def registerController(controller:Controller):Unit = {
    actions = actions ++ controller.definitions.groupBy(_.method)
  }
}