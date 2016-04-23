package se.chimps.bitziness.core.endpoints.http.server.unrouting

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
  def Moved():HttpResponse = HttpResponse(StatusCodes.TemporaryRedirect)

  implicit def viewImplicit(response:HttpResponse):ViewExplicitImplicit = new ViewExplicitImplicit(response)
}

class ViewExplicitImplicit(response:HttpResponse) {

  def withView(view:View):HttpResponse = {
    val charSet = HttpCharset.custom(view.charset)

    val contentType = MediaType.parse(view.contentType) match {
      case Left(errors) => {
        None
      }
      case Right(mediaType) => Some(mediaType.withCharset(charSet))
    }

    if (contentType.isEmpty) {
      response.withEntity(HttpEntity(view.render()))
    } else {
      response.withEntity(contentType.get, view.render())
    }
  }
}