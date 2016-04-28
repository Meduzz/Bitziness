package se.chimps.bitziness.core.generic

import se.chimps.bitziness.core.services.healthcheck.HealthChecks

/**
	* A trait to use to add healthchecks.
	*/
trait HealthCheck {
	/**
		* Register a healthcheck.
		* @param name the name.
		* @param check the function to execute.
		*/
	def healthCheck(name:String, check:()=>Boolean):Unit = HealthChecks.register(name, check)
}
