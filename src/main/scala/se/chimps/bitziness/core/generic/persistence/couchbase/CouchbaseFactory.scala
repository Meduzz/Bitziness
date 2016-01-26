package se.chimps.bitziness.core.generic.persistence.couchbase

import com.couchbase.client.java.env.DefaultCouchbaseEnvironment
import com.couchbase.client.java.{Bucket, CouchbaseCluster}
import com.couchbase.client.java.cluster.ClusterManager
import se.chimps.bitziness.core.generic.Configs
import collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

trait CouchbaseFactory extends Configs {

  val nodes = asStringList("couchbase.nodes")
  val env = DefaultCouchbaseEnvironment.builder()
      .build()

  lazy val couchbase = CouchbaseCluster.create(env, nodes.asJava)

  def withClusterManager(username:String, password:String)(op:(ClusterManager)=>Unit):Unit = {
    val cm = couchbase.clusterManager(username, password)
    op(cm)
  }

  def withBucket[T](name:String, password:String)(op:(Bucket)=>T)(implicit ec:ExecutionContext):Future[T] = Future {
    val bucket = couchbase.openBucket(name, password)
    op(bucket)
  }

  def withBucket[T](name:String)(op:(Bucket)=>T)(implicit ec:ExecutionContext):Future[T] = Future {
    val bucket = couchbase.openBucket(name)
    op(bucket)
  }
}
