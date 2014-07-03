package se.chimps.bitziness.core.service.plugins.rest.exception

/**
 * Signal that the route was not found.
 */
class RouteNotFoundException(val message:String) extends Exception(message) {
}
