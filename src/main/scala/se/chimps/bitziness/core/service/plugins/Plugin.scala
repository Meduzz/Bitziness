package se.chimps.bitziness.core.service.plugins

import se.chimps.bitziness.core.service.Service

/**
 * Base trait for Plugins.
 * A plugin is used by individual services to enhance themselfs. It will mostly be related to exposing endpoints. But could really be anything.
 */
trait Plugin { self:Service =>

}
