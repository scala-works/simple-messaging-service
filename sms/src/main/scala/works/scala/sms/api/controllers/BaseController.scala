package works.scala.sms.api.controllers

import works.scala.sms.api.models.ApiError
import zio.*
import zio.http.endpoint.*

trait BaseController:
  val routes: List[Routes[Any, ApiError, EndpointMiddleware.None]] = List.empty

  extension [T](task: Task[T])
    def handleErrors: ZIO[Any, ApiError, T]           =
      task.mapError(e => ApiError(e.getMessage()))
    def handleErrorsEither: Task[Either[ApiError, T]] =
      handleErrors.either
