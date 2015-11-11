package se.chimps.bitziness.core.endpoints.http.client

import akka.http.scaladsl.model._

import scala.collection.immutable._

/**
 *
 */
trait RequestBuilders {

  def GET(url:String, headers:Seq[HttpHeader] = Seq()):HttpRequest = request("GET", url, None, headers)

  def POST(url:String, body:Option[String] = None, headers:Seq[HttpHeader] = Seq()):HttpRequest = request("POST", url, body, headers)

  def PUT(url:String, body:Option[String] = None, headers:Seq[HttpHeader] = Seq()):HttpRequest = request("PUT", url, body, headers)

  def DELETE(url:String, body:Option[String] = None, headers:Seq[HttpHeader] = Seq()):HttpRequest = request("DELETE", url, body, headers)

  def HEAD(url:String, headers:Seq[HttpHeader] = Seq()):HttpRequest = request("HEAD", url, None, headers)

  def PATCH(url:String, body:Option[String] = None, headers:Seq[HttpHeader] = Seq()):HttpRequest = request("PATCH", url, body, headers)

  def request(method:String, url:String, body:Option[String] = None, headers:Seq[HttpHeader] = Seq()):HttpRequest = {
    HttpRequest(method = HttpMethods.getForKey(method).getOrElse(HttpMethods.GET), uri = Uri.apply(url), entity = body.map(HttpEntity(_)).getOrElse(HttpEntity.Empty), headers = headers)
  }
}
