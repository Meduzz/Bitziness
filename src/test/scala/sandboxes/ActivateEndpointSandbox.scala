package sandboxes

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.{PipeToSupport, ask}
import akka.util.Timeout
import net.fwbrasil.activate.ActivateContext
import net.fwbrasil.activate.entity.Entity
import net.fwbrasil.activate.migration.Migration
import net.fwbrasil.activate.storage.memory.TransientMemoryStorage
import se.chimps.bitziness.core.Service
import se.chimps.bitziness.core.generic.Init
import se.chimps.bitziness.core.endpoints.persistence.activate.endpoint.ActivateEndpoint

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}

object InMemoryContext extends ActivateContext {
  override val storage = new TransientMemoryStorage
}

object ActivateEndpointSandbox extends App {
  val system = ActorSystem("ActivateSandbox")
  val service = system.actorOf(Props(classOf[FakeActivateService]))
  implicit val timeout = Timeout(3L, TimeUnit.SECONDS)
  implicit val context = InMemoryContext

  service ! Init

  val p = for {
    person <- (service ? Store("Bosse Bus", "Bosse", 100)).mapTo[Person]
    get <- (service ? Fetch(person.id)).mapTo[Person]
  } yield get

  p.foreach(s => println(s))

  val f = Future {
    Thread.sleep(1500L)
  }
  Await.result(f, Duration(3l, TimeUnit.SECONDS))

  system.terminate()

  class PersonMigration extends Migration {
    override def timestamp: Long = 1L

    override def up: Unit = {
      table[Person].createTable(_.column[String]("name"), _.column[String]("nicknane"), _.column[Int]("age"))
    }
  }
}

class FakeActivateService extends Service with PipeToSupport {
  var ep:ActorRef = _
  implicit val timeout = Timeout(3L, TimeUnit.SECONDS)

  override def handle: Receive = {
    case any:AnyRef => {
      val caller = sender()
      (ep ? any).pipeTo(caller)
    }
  }

  override def initialize(): Unit = {
    ep = initEndpoint(classOf[ActivateEndpointSandbox], "ActivateEndpoint")
  }
}

class ActivateEndpointSandbox extends ActivateEndpoint with PipeToSupport {
  implicit val timeout = Timeout(3L, TimeUnit.SECONDS)

  override def dbContext: ActivateContext = InMemoryContext

  override def receive: Actor.Receive = {
    case Store(name, nickname, age) => {
      val caller = sender()
      val promise = Promise[Person]()

      val traj = tryTransaction[Person](() => {
        new Person(name, nickname, age)
      })

      promise.complete(traj)

      promise.future.pipeTo(caller)
    }
    case Fetch(id) => {
      val caller = sender()
      val promise = Promise[Person]()

      val traj = tryTransaction[Person](() => {
        import InMemoryContext._
        select[Person].where(_.id :== id).head
      })

      promise.complete(traj)

      promise.future.pipeTo(caller)
    }
  }
}

class Person(val name:String, val nickname:String, val age:Int) extends Entity
case class Store(name:String, nickname:String, age:Int)
case class Fetch(id:String)
