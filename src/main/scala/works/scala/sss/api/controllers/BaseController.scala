package works.scala.sss.api.controllers

import caliban.GraphQL
import works.scala.sss.api.models.ApiError
import zio.*
import zio.http.endpoint.*

trait BaseController:
  val routes: List[Routes[Any, ApiError, EndpointMiddleware.None]] = List.empty
  val graphs: List[GraphQL[Any]]                                   = List.empty

  extension [T](task: Task[T])
    def handleErrors: ZIO[Any, ApiError, T]           =
      task.mapError(e => ApiError(e.getMessage()))
    def handleErrorsEither: Task[Either[ApiError, T]] =
      handleErrors.either
