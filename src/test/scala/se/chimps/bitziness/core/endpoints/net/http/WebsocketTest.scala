package se.chimps.bitziness.core.endpoints.net.http

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws._
import akka.pattern.{PipeToSupport, ask}
import akka.stream.scaladsl.{Flow, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.testkit.TestKitBase
import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import se.chimps.bitziness.core.endpoints.net.http.client.RequestBuilders
import se.chimps.bitziness.core.endpoints.net.http.server.unrouting.{Action, Controller, ResponseBuilders}

import scala.concurrent.Future

class WebsocketTest extends FunSuite with TestKitBase with BeforeAndAfterAll with ScalaFutures {
	implicit lazy val system = ActorSystem("WebSocketz")
	implicit val ec = system.dispatcher
	implicit val timeout = Timeout(3L, TimeUnit.SECONDS)

	val server = system.actorOf(Props(classOf[WsServer]))

	test("websocket ping pong") {
		val client = system.actorOf(Props(classOf[WsClient]))

		val resp = client ? Ping

		whenReady(resp.mapTo[String], timeout(Span(3L, Seconds))) { text =>
			assert(text.equals("pong"))
		}
	}

	override protected def afterAll(): Unit = {
		super.afterAll()
		system.terminate()
	}
}

class WsServer extends HttpServerEndpoint {
	implicit val ec = context.dispatcher
	implicit val materializer = ActorMaterializer()(context)
	implicit val system = context.system

	override def createServer(builder: HttpServerBuilder): Future[ActorRef] = {
		builder.listen("127.0.0.1", 8080).build()
	}

	override def receive: Receive = {
		case _ => println("unhandled message in WsServer")
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
					TextMessage("pong")
				}
				case b:BinaryMessage => {
					BinaryMessage(Source.empty)
				}
				case a:TextMessage => {
					TextMessage(Source.empty)
				}
			}, Error().withEntity("No upgrade header."), Some("text"))
		} else {
			Error().withEntity("Request was not for websockets...")
		}
	})
}

class WsClient extends WsClientEndpoint with HttpClientEndpoint with RequestBuilders with PipeToSupport {

	implicit val timeout = Timeout(3L, TimeUnit.SECONDS)

	override def bufferSize: Int = 10

	override def overflowStrategy: OverflowStrategy = OverflowStrategy.dropNew

	var caller:ActorRef = _

	/**
		* Setting up the flow should be fairly straight forward:
		* Http().webSocketClientFlow(...)
		*
		* @return
		*/
	override def buildConnection(): Flow[Message, Message, Future[WebSocketUpgradeResponse]] = {
		Http()(context.system).webSocketClientFlow(WebSocketRequest("ws://127.0.0.1:8080/ws", subprotocol = Some("text")))
	}

	override def setupConnection(builder: ConnectionBuilder): ActorRef = {
		builder.host("127.0.0.1", 8080).build(false)
	}

	override def receive: Receive = {
		case Ping => {
			caller = sender()
			onSend(TextMessage("ping"))
		}
		case m:TextMessage => m.textStream.runFold("")((a, b) => a + b).pipeTo(caller)
		case s => println(s"client received $s")
	}
}

case object Ping