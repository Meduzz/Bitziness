package se.chimps.bitziness.core.endpoints.net.http

import akka.Done
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{Message, WebSocketUpgradeResponse}
import akka.stream.scaladsl._
import akka.stream.{Materializer, OverflowStrategy, QueueOfferResult}
import se.chimps.bitziness.core.Endpoint

import scala.concurrent.Future

/**
	* An endpoint that speaks websocket.
	*/
trait WsClientEndpoint extends Endpoint {

	implicit def materializer:Materializer
	implicit val ec = context.dispatcher

	def bufferSize:Int
	def overflowStrategy:OverflowStrategy

	private val source = Source.queue[Message](bufferSize, overflowStrategy)
	private val sink = Sink.foreach[Message](msg => self ! msg)

	var queue:SourceQueueWithComplete[Message] = _

	/**
		* Setting up the flow should be fairly straight forward:
		* Http().webSocketClientFlow(...)
		*
		* @return
		*/
	def buildConnection():Flow[Message, Message, Future[WebSocketUpgradeResponse]]

	def onSend(message:Message):Future[QueueOfferResult] = queue.offer(message)

	def onError(e:Throwable):Unit = queue.fail(e)

	def onComplete():Unit = {
		queue.complete()
	}

	@throws[Exception](classOf[Exception])
	override def preStart(): Unit = {
		super.preStart()

		val ((theQueue, response), closed) = source.viaMat(buildConnection())(Keep.both).toMat(sink)(Keep.both).run()(materializer)

		response.flatMap { res =>
			if (res.response.status == StatusCodes.SwitchingProtocols) {
				Future.successful(Done)
			} else {
				// TODO let implementors deal with this somehow.
				throw new RuntimeException(s"Upgrade to websockets failed. (${res.response.status})")
			}
		}

		queue = theQueue
	}
}