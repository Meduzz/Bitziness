package se.chimps.bitziness.core.generic.persistence.neo4j

import java.io.File

import org.neo4j.graphdb.GraphDatabaseService
import org.neo4j.graphdb.factory.GraphDatabaseFactory

object Neo4jSingleton {
  private var graphDbs = Map[String, GraphDatabaseService]()

  def apply(dataPath:String):GraphDatabaseService = {
    if (!graphDbs.contains(dataPath)) {
      val db = new GraphDatabaseFactory().newEmbeddedDatabase(new File(dataPath));
      graphDbs = graphDbs ++ Map(dataPath -> db)

      db
    } else {
      graphDbs(dataPath)
    }
  }

  def apply(dataPath:String, propertiesPath:String):GraphDatabaseService = {
    if (!graphDbs.contains(dataPath)) {
      val db = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(new File(dataPath))
        .loadPropertiesFromFile(propertiesPath)
        .newGraphDatabase()
      graphDbs = graphDbs ++ Map(dataPath -> db)

      db
    } else {
      graphDbs(dataPath)
    }
  }

  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run(): Unit = {
      graphDbs.values.foreach(_.shutdown())
    }
  })
}
