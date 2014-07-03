package se.chimps.bitziness.core.project.modules

import se.chimps.bitziness.core.project.Project

/**
 * Base trait for modules.
 * A module are a piece of project wide feature and can only be combined with the Project base class.
 * It can be features like collecting metrics or handling DI etc.
 */
trait Module { self:Project =>

}
