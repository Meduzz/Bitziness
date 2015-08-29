package se.chimps.bitziness.core.endpoints.http.client

import scala.collection.immutable.Seq

import akka.http.scaladsl.model._

/**
 *
 */
trait RequestBuilders {

  // TODO add methods for all http verbs

  def request(method:String, url:String, body:Option[String] = None, headers:Seq[HttpHeader] = Seq()):HttpRequest = {
    HttpRequest(method = HttpMethods.getForKey(method).getOrElse(HttpMethods.GET), uri = Uri.apply(url), entity = body.fold(HttpEntity.Empty)(HttpEntity.apply), headers = headers)
  }
}
