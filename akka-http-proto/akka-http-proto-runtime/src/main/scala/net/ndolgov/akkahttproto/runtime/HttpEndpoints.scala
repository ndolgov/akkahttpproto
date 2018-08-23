package net.ndolgov.akkahttproto.runtime

import akka.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, MediaType, MediaTypes, StatusCode, StatusCodes}
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.{ExceptionHandler, Rejection, RejectionHandler}
import org.slf4j.Logger

object HttpEndpoints {
  val JSON: MediaType.WithFixedCharset = MediaTypes.`application/json`

  /** @return a handler to be used for really unexpected exceptions uncaught by other means */
  def unexpectedExceptionHandler(log: Logger): ExceptionHandler =
    ExceptionHandler {
      case e: Exception =>
        log.error("Unexpected error", e)
        complete(httpErrorResponse(StatusCodes.InternalServerError, "Unexpected error: " + e.getMessage))
    }

  /** @return a handler to be used for request messages that cannot be deserialized (e.g. wrong message or garbled JSON) */
  def garbledRequestHandler(log: Logger): RejectionHandler =
    RejectionHandler.newBuilder().
      handleAll[Rejection] { rejection =>
      log.error(s"Could not parse request because of $rejection")
      complete(httpErrorResponse(StatusCodes.BadRequest, "Could not parse request"))
    }.result()

  def httpErrorResponse(status : StatusCode, message: String): HttpResponse = {
    HttpResponse(
      status,
      entity = HttpEntity(ContentType(JSON), message)
    )
  }
}
