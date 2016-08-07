Bitzinezz
=========

The microservice framework.

Bitzinezz are shamelessly influenced by the excellent Dropwizard framework, but built on top of Akka, Spray and other less 1998-style frameworks.
Almost everything in Bitzinezz is an actor.

A Bitzinezz app will have one App with a main-method. The app register one or more services, whom in turn can have multiple endpoints.

A service is were you run your buisness-logic and call other services and databases and what not. Communication are thought
to be done through endpoints. These endpoints are registered on the service.

An endpoint are the edge of your app. This is where data comes in and sometimes leave your service.

It all starts with your App. It might look something like this:

```
import se.chimps.bitziness.core.BitzinessApp

object TestService extends BitzinessApp {
  override def initialize(args:Array[String]):Unit = {
    initService[PingService](classOf[PingService], "Ping")
  }
}
```

Here you register your services. The BitzinessApp has a main(args:Array[String]) so it will be the starting point of your app.

Next you'll go ahead a create a service. In this example, it only registers a rest-endpoint.
And the only business logic are the PING that bounces back as a PONG.

```
import se.chimps.bitziness.core.{Service}
import se.chimps.bitziness.core.endpoints.net.rest.{EndpointDefinition, RestEndpointBuilder, RESTEndpoint}

class PingService extends Service {
  override def handle:Receive = {
    case "PING" => sender() ! "PONG"
  }

  override def initialize():Unit = {
    initEndpoint[PingEndpoint](classOf[PingEndpoint], "Http")
  }
}
```

Now we only need the endpoint, it can look like this. This type of endpoint also delegates further and has a controller defined.

```
import akka.actor.ActorRef
import akka.util.Timeout
import se.chimps.bitziness.core.endpoints.net.rest.spray.unrouting.Action
import se.chimps.bitziness.core.endpoints.net.rest.spray.unrouting.Framework.Controller
import se.chimps.bitziness.core.endpoints.net.rest.spray.unrouting.Model.Responses.Ok
import se.chimps.bitziness.core.endpoints.net.rest.spray.unrouting.view.Scalate
import se.chimps.bitziness.core.generic.Waitable._
import se.chimps.bitziness.core.{Service}
import akka.pattern._

class PingEndpoint(val service:ActorRef) extends RESTEndpoint {
  override def configureRestEndpoint(builder:RestEndpointBuilder):EndpointDefinition = {
    val controller = new PingController(self)
    builder.mountController("", controller)
    builder.build()
  }

  override def receive:Receive = {
    case s:String =>
      implicit val timeout = Timeout(3l, TimeUnit.SECONDS)
      val sender:ActorRef = context.sender()
      val pong = (service ? s.toUpperCase).get[String].toLowerCase
      sender ! pong
  }
}

class PingController(val endpoint:ActorRef) extends Controller {
  override def apply(service:ActorRef):Unit = {
    get("/", Action { req =>
      Ok().sendView(Scalate("/templates/hello.jade", Map("title"->"Hello world!"))).build()
    })
    get("/ping", Action { req =>
      implicit val timeout = Timeout(3l, TimeUnit.SECONDS)
      val pong = (service ? "ping").get[String]
      Ok().sendView(Scalate("/templates/ping.jade", Map("pong" -> pong))).build()
    })
    get("/hello/:world", Action { req =>
      Ok().sendView(Scalate("/templates/world.jade", Map("world" -> req.params("world").getOrElse("failed")))).build()
    })
  }

  implicit def str2bytes(data:String):Array[Byte] = {
    data.getBytes("utf-8")
  }
}
```

Lets get down to Bitzinezz shall we!?