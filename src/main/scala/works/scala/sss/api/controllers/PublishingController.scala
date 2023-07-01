package works.scala.sss.api.controllers

import works.scala.sss.api.models.*
import works.scala.sss.api.services.PublishingService
import zio.*
import zio.http.*
import zio.http.codec.HttpCodec
import zio.http.endpoint.*

object PublishingController:
  val layer: ZLayer[PublishingService, Nothing, PublishingController] = ZLayer {
    ZIO.service[PublishingService].map(PublishingController.apply)
  }

case class PublishingController(publishingService: PublishingService)
    extends BaseController:
  import HttpCodec.*

  val publishMessage =
    Endpoint
      .post("publish" / string("topic"))
      .in[PublishMessageRequest]
      .out[PublishMessageResponse]
      .outError[ApiError](Status.InternalServerError)
      .implement((topic, in) =>
        publishingService.publishMessage(topic, in).handleErrors
      )

  override val routes: List[Routes[Any, ApiError, EndpointMiddleware.None]] =
    List(
      publishMessage
    )
