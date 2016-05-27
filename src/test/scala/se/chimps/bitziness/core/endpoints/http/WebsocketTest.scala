package se.chimps.bitziness.core.endpoints.http

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws._
import akka.stream.scaladsl.{Flow, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.testkit.TestKitBase
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import se.chimps.bitziness.core.endpoints.http.client.RequestBuilders
import se.chimps.bitziness.core.endpoints.http.server.unrouting.{Action, Controller, ResponseBuilders}

import scala.concurrent.Future

class WebsocketTest extends FunSuite with TestKitBase with BeforeAndAfterAll with ScalaFutures {
	implicit lazy val system = ActorSystem("WebSocketz")
	implicit val ec = system.dispatcher

	val server = system.actorOf(Props(classOf[WsServer]))

	test("websocket ping pong") {
		val client = system.actorOf(Props(classOf[WsClient]))

		whenReady(Future(Thread.sleep(140))) { unit =>
			print(".")
		}
		whenReady(Future(Thread.sleep(140))) { unit =>
			print(".")
		}
		whenReady(Future(Thread.sleep(140))) { unit =>
			print(".")
		}
		whenReady(Future(Thread.sleep(140))) { unit =>
			print(".")
		}
		whenReady(Future(Thread.sleep(140))) { unit =>
			print(".")
		}

		client ! Ping

		whenReady(Future(Thread.sleep(140))) { unit =>
			print(".")
		}
		whenReady(Future(Thread.sleep(140))) { unit =>
			print(".")
		}
		whenReady(Future(Thread.sleep(140))) { unit =>
			println(".")
		}
		whenReady(Future(Thread.sleep(140))) { unit =>
			print(".")
		}
		whenReady(Future(Thread.sleep(140))) { unit =>
			println(".")
		}

		whenReady(Future(Thread.sleep(140))) { unit =>
			print(".")
		}
		whenReady(Future(Thread.sleep(140))) { unit =>
			print(".")
		}
		whenReady(Future(Thread.sleep(140))) { unit =>
			println(".")
		}
		whenReady(Future(Thread.sleep(140))) { unit =>
			print(".")
		}
		whenReady(Future(Thread.sleep(140))) { unit =>
			println(".")
		}
	}

	override protected def afterAll(): Unit = {
		super.afterAll()
		system.terminate()
	}
}

class WsServer extends HttpServerEndpoint {
	implicit val ec = context.dispatcher

	override def createServer(builder: HttpServerBuilder): Future[ActorRef] = {
		builder.listen("127.0.0.1", 8080).build()
	}

	override def receive: Receive = {
		case _ => println("ping")
	}

	@throws[Exception](classOf[Exception])
	override def preStart(): Unit = {
		super.preStart()
		registerController(new WsController)
	}
}

class WsController extends Controller with ResponseBuilders {
	import scala.concurrent.ExecutionContext.Implicits.global

	get("/ws", Action.sync { req =>
		if (req.isUpgrade) {
			req.handleWebsocket({
				case TextMessage.Strict("ping") => {
					println("There's text.")
					TextMessage("pong")
				}
				case b:BinaryMessage => {
					BinaryMessage(Source.empty)
				}
				case a:TextMessage => {
					println(a)
					TextMessage(Source.empty)
				}
			}, Error().withEntity("No upgrade header."))
		} else {
			Error().withEntity("Request was not for websockets...")
		}
	})
	get("/spam", Action.sync { req =>
		Ok().withEntity("spam")
	})
}

class WsClient extends WsClientEndpoint with HttpClientEndpoint with RequestBuilders {
	implicit override val materializer = ActorMaterializer()(context)

	override def bufferSize: Int = 10

	override def overflowStrategy: OverflowStrategy = OverflowStrategy.dropNew

	/**
		* Setting up the flow should be fairly straight forward:
		* Http().webSocketClientFlow(...)
		*
		* @return
		*/
	override def buildConnection(): Flow[Message, Message, Future[WebSocketUpgradeResponse]] = {
		Http()(context.system).webSocketClientFlow(WebSocketRequest("ws://127.0.0.1:8080/ws"))
	}

	override def setupConnection(builder: ConnectionBuilder): ActorRef = {
		builder.host("127.0.0.1", 8080).build(false)
	}

	override def receive: Receive = {
		case Ping => {
			println("There's a PING")
//			onSend(TextMessage("ping"))
			send(GET("/spam")).map(res => println(res))
		}
		case s => println(s)
	}
}

case object Ping