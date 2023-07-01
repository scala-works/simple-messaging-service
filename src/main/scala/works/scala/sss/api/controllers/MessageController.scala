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

object MessageController:
  val layer: ZLayer[ConsumerService & Connection, Nothing, MessageController] =
    ZLayer {
      for {
        svc  <- ZIO.service[ConsumerService]
        conn <- ZIO.service[Connection]
      } yield MessageController(svc, conn)
    }

case class MessageController(
    consumerService: ConsumerService,
    rmqConnection: Connection
) extends BaseController:
  import HttpCodec.*

  val consumeOne =
    Endpoint
      .get("messages" / string("subscritpion"))
      .out[MessageConsumeResponse]
      .outError[ApiError](Status.InternalServerError)
      .implement(sub => consumerService.ackingConsume(sub).handleErrors)

  val socketApp = Http.collectZIO[Request] {
    case Method.GET -> Root / "stream" / subscription =>
      consumerService.handleWs(subscription).toResponse
  }

  override val routes: List[Routes[Any, ApiError, EndpointMiddleware.None]] =
    List(
      consumeOne
    )
