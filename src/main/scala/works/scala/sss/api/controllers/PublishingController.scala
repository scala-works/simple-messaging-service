package works.scala.sss.api.controllers

import sttp.tapir.*
import works.scala.sss.api.models.{
  PublishMessageRequest,
  PublishMessageResponse
}
import works.scala.sss.api.services.PublishingService
import zio.*

object PublishingController:
  val layer: ZLayer[PublishingService, Nothing, PublishingController] = ZLayer {
    ZIO.service[PublishingService].map(PublishingController.apply)
  }

case class PublishingController(publishingService: PublishingService)
    extends BaseController {}

  // private val publishEndpoint =
  //   baseEndpoint
  //     .in("publish")
  //     .tag("publish")

  // val publishMessage =
  //   publishEndpoint
  //     .name("publishMessage")
  //     .description("Publish a message to a topic")
  //     .post
  //     .in(path[String]("topic"))
  //     .in(jsonBody[PublishMessageRequest])
  //     .out(jsonBody[PublishMessageResponse])
  //     .serverLogic(in =>
  //       publishingService.publishMessage(in._1, in._2).handleErrors
  //     )

  // override val routes: List[ServerEndpoint[Any, Task]] = List(
  //   publishMessage
  // )
