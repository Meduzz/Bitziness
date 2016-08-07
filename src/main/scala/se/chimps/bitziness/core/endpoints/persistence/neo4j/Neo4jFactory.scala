package se.chimps.bitziness.core.endpoints.persistence.neo4j

import org.neo4j.graphdb.GraphDatabaseService
import se.chimps.bitziness.core.generic.Configs

import scala.concurrent.{Promise, Future}
import scala.util.{Failure, Success, Try}

trait Neo4jFactory extends Configs {

  def dbPath:String
  def propsPath:Option[String] = None

  def tryTransaction[T](op:(GraphDatabaseService)=>T):Try[T] = {
    val db = propsPath.map(Neo4jSingleton(dbPath, _)).getOrElse(Neo4jSingleton(dbPath))

    synchronized({
      val tx = db.beginTx()

      Try(op(db)) match {
        case s:Success[T] => tx.success(); s
        case f:Failure[T] => tx.failure(); f
      }
    })
  }

  def asyncTransaction[T](op:(GraphDatabaseService)=>T):Future[T] = {
    val promise = Promise[T]()
    val db = propsPath.map(Neo4jSingleton(dbPath, _)).getOrElse(Neo4jSingleton(dbPath))

    synchronized({
      val tx = db.beginTx()

      Try[T](op(db)) match {
        case Success(s) => tx.success(); promise.success(s)
        case Failure(e) => tx.failure(); promise.failure(e)
      }
    })

    promise.future
  }
}
