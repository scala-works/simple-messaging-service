package works.scala.sss.api.controllers

import com.rabbitmq.client.Connection
import works.scala.sss.api.models.*
import works.scala.sss.api.services.ConsumerService
import works.scala.sss.rmq.RMQ
import zio.http.ChannelEvent.*
import zio.http.*
import zio.stream.*
import zio.*
import zio.Duration.*
import zio.http.ChannelEvent.UserEvent.HandshakeComplete
import works.scala.sss.extensions.Extensions.*
import zio.http.codec.HttpCodec
import zio.http.endpoint.*
import java.util.UUID
import scala.language.postfixOps

object ConsumerController:
  val layer: ZLayer[ConsumerService & Connection, Nothing, ConsumerController] =
    ZLayer {
      for {
        svc  <- ZIO.service[ConsumerService]
        conn <- ZIO.service[Connection]
      } yield ConsumerController(svc, conn)
    }

case class ConsumerController(
    consumerService: ConsumerService,
    rmqConnection: Connection
) extends BaseController:
  import HttpCodec.*

  val consumeOne =
    Endpoint
      .get("messages" / string("subscription"))
      .out[MessageConsumeResponse]
      .outError[ApiError](Status.InternalServerError)
      .implement(sub => consumerService.ackingConsume(sub).handleErrors)

  val socketApp = Http.collectZIO[Request] {
    case req @ Method.GET -> Root / "stream" / subscription =>
      consumerService
        .handleWs(
          subscription,
          req.url.queryParams
            .get("preFetch")
            .map(_.mkString.toInt)
            .getOrElse(1),
          req.url.queryParams
            .get("autoAck")
            .map(_.mkString.toBoolean)
            .getOrElse(false)
        )
        .toResponse
  }

  override val routes: List[Routes[Any, ApiError, EndpointMiddleware.None]] =
    List(
      consumeOne
    )
