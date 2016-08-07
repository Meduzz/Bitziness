package se.chimps.bitziness.core.endpoints.net.http.server.unrouting

import akka.http.scaladsl.model._
import se.chimps.bitziness.core.generic.View

/**
 *
 */
trait ResponseBuilders {

  def Ok():HttpResponse = HttpResponse(StatusCodes.OK)
  def Created():HttpResponse = HttpResponse(StatusCodes.Created)
  def NotFound():HttpResponse = HttpResponse(StatusCodes.NotFound)
  def Error():HttpResponse = HttpResponse(StatusCodes.InternalServerError)
  def TODO():HttpResponse = HttpResponse(StatusCodes.NotImplemented)
  def Forbidden():HttpResponse = HttpResponse(StatusCodes.Forbidden)
  def BadRequest():HttpResponse = HttpResponse(StatusCodes.BadRequest)
  def NoContent():HttpResponse = HttpResponse(StatusCodes.NoContent)
  def Moved():HttpResponse = HttpResponse(StatusCodes.custom(301, "Moved"))

  implicit def viewImplicit(response:HttpResponse):ViewExplicitImplicit = new ViewExplicitImplicit(response)
}

class ViewExplicitImplicit(response:HttpResponse) {

  def withView(view:View):HttpResponse = {
    val charSet = HttpCharset.custom(view.charset)

    val contentType = ContentType.parse(view.contentType) match {
			case Left(error) => None
			case Right(content) => Some(content)
		}

    if (contentType.isEmpty) {
      response.withEntity(HttpEntity(view.render()))
    } else {
      response.withEntity(contentType.get, view.render())
    }
  }
}