package se.chimps.bitziness.core.generic

import java.util.UUID

import se.chimps.bitziness.core.endpoints.http.server.unrouting.Controller
import se.chimps.bitziness.core.generic.serializers.JSONSerializer

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future

trait SessionSupport { ctrl:Controller =>
  def sessionId:String = "sessionId"
  implicit def sessionFactory:SessionFactory
}

object SessionStore {
  def apply(sessionId:String)(implicit sessionFactory:SessionFactory):SessionStore = {
    sessionFactory.load(sessionId)
  }

  def create()(implicit sessionFactory: SessionFactory):Tuple2[String, SessionStore] = {
    val sessionId = UUID.randomUUID().toString
    (sessionId, sessionFactory.load(sessionId))
  }

  def delete(sessionId:String)(implicit sessionFactory: SessionFactory):Unit = {
    sessionFactory.destroy(sessionId)
  }
}

trait SessionStore {
  def set[T](key:String, value:T):Future[Boolean]
  def get[T](key:String)(implicit tag:Manifest[T]):Future[Option[T]]
  def isSet(key:String):Future[Boolean]
}

trait SessionFactory {
  def load(session:String):SessionStore
  def destroy(session:String):Boolean
}

object LocalSession {

  object LocalSessionFactory extends SessionFactory {
    private val sessions = TrieMap[String, TrieMap[String, Any]]()

    override def load(session: String): SessionStore = {
      if (!sessions.contains(session)) {
        sessions += (session -> TrieMap[String, Any]())
      }

      new LocalSessionStore(sessions(session))
    }

    override def destroy(session: String): Boolean = {
      sessions(session).clear()
      sessions.remove(session)
      true
    }
  }

  class LocalSessionStore(val session:TrieMap[String, Any]) extends SessionStore with JSONSerializer {
    import scala.concurrent.ExecutionContext.Implicits.global

    override def set[T](key: String, value: T): Future[Boolean] = Future {
      session += (key -> value)
      true
    }

    override def get[T](key: String)(implicit tag:Manifest[T]): Future[Option[T]] = Future {
      session.get(key) match {
        case Some(string) => {
          Some[T](string.asInstanceOf[T])
        }
        case None => None
      }
    }

    override def isSet(key: String): Future[Boolean] = Future {
      session.contains(key)
    }
  }
}