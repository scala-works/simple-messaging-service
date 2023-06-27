package works.scala.sss.api.controllers

import caliban.GraphQL
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.ServerEndpoint
import works.scala.sss.api.models.ApiError
import zio.*

trait BaseController:
  val routes: List[ServerEndpoint[Any, Task]] = List.empty
  val graphs: List[GraphQL[Any]]              = List.empty

  extension [T](task: Task[T])
    def handleErrors: Task[Either[ApiError, T]] =
      task.mapError(e => ApiError(e.getMessage)).either

  val baseEndpoint: Endpoint[Unit, Unit, ApiError, Unit, Any] =
    endpoint
      .errorOut(jsonBody[ApiError])
