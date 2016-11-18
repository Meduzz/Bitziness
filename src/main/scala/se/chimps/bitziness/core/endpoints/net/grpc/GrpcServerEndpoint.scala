package se.chimps.bitziness.core.endpoints.net.grpc

import io.grpc.Server
import se.chimps.bitziness.core.Endpoint

/**
	* Setup your server, but dont worry about starting it.
	* That will be done automatically..
	* Shutdown will be called on server before calling stop.
	*
	* TODO test... after proto3 + grpc compilation has been sorted.
	*/
trait GrpcServerEndpoint extends Endpoint {

	val server:Server = setupServer()

	def setupServer():Server

	def start():Unit = {}
	def stop():Unit

	@scala.throws[Exception](classOf[Exception])
	override def preStart():Unit = {
		super.preStart()
		server.start()
		// TODO do we need to call server.awaitTermination()?
		start()
	}

	@scala.throws[Exception](classOf[Exception])
	override def postStop():Unit = {
		server.shutdown()
		stop()
		super.postStop()
	}
}
