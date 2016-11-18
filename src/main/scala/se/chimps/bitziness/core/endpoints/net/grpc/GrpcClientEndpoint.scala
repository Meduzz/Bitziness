package se.chimps.bitziness.core.endpoints.net.grpc

import io.grpc.ManagedChannel
import se.chimps.bitziness.core.Endpoint

/**
	* Once you setup your channel, you can in start()
	* setup your stub, and start using it from receive.
	* Channel will be shutdown before calling stop().
	*
	* TODO test... after proto3 + grpc compilation has been sorted.
	*/
trait GrpcClientEndpoint extends Endpoint {

	val channel:ManagedChannel = setupChannel()

	def setupChannel():ManagedChannel

	def start():Unit
	def stop():Unit

	@scala.throws[Exception](classOf[Exception])
	override def preStart():Unit = {
		super.preStart()
		start()
	}

	@scala.throws[Exception](classOf[Exception])
	override def postStop():Unit = {
		channel.shutdown()
		stop()
		super.postStop()
	}
}
