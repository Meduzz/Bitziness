package se.chimps.bitziness.core.generic

import java.util.concurrent.TimeUnit

import akka.actor.{Props, ActorSystem, Actor}
import akka.util.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class ActorLoaderTest extends FunSuite with ActorLoader with BeforeAndAfterAll with ScalaFutures {

  implicit val system:ActorSystem = ActorSystem("ActorLoading")
  implicit val timeout = Timeout(1L, TimeUnit.SECONDS)

  test("Start one, find one") {
    val first = system.actorOf(Props(classOf[MyActor]), "First")

    whenReady(loadOrCreate[MyActor]("/user/First", "First")) { second =>
      assert(second.path.equals(first.path))
    }
  }

  test("starting with an empty sheet") {
    val first = whenReady(loadOrCreate[MyActor]("/user/Second", "Second")) { second =>
      second
    }

    whenReady(loadOrCreate[MyActor]("/user/Second", "Second")) { second =>
      assert(second.path.equals(first.path))
    }
  }

  override protected def afterAll():Unit = {
    super.afterAll()
    system.shutdown()
  }
}

class MyActor extends Actor {
  override def receive:Receive = {
    case any => println(any)
  }
}