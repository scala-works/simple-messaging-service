package works.scala.sms.api.controllers

import works.scala.sms.api.models.*
import works.scala.sms.api.services.TopicService
import zio.*
import zio.http.*
import zio.http.codec.HttpCodec
import zio.http.endpoint.*

object TopicController:
  val layer: ZLayer[TopicService, Nothing, TopicController] = ZLayer {
    ZIO.service[TopicService].map(TopicController.apply)
  }

case class TopicController(topicService: TopicService) extends BaseController:
  import HttpCodec._

  val getTopics: Routes[Any, ApiError, EndpointMiddleware.None] =
    Endpoint
      .get("topics")
      .out[GetTopicsResponse]
      .outError[ApiError](Status.InternalServerError)
      .implement(_ => topicService.getTopics.handleErrors)

  val getTopic =
    Endpoint
      .get("topics" / string("name"))
      .out[GetTopicResponse]
      .outError[ApiError](Status.InternalServerError)
      .implement(name => topicService.getTopic(name).handleErrors)

  val createTopic =
    Endpoint
      .post("topics")
      .in[CreateTopicRequest]
      .out[CreateTopicResponse]
      .outError[ApiError](Status.InternalServerError)
      .implement(in => topicService.createTopic(in).handleErrors)

  val deleteTopic =
    Endpoint
      .delete("topics" / string("name"))
      .out[DeleteTopicResponse]
      .outError[ApiError](Status.InternalServerError)
      .implement(name => topicService.deleteTopic(name).handleErrors)

  override val routes: List[Routes[Any, ApiError, EndpointMiddleware.None]] =
    List(
      getTopics,
      getTopic,
      createTopic,
      deleteTopic
    )
