package works.scala.sss.api.controllers

import caliban.*
import caliban.schema.Schema.auto.*
import caliban.schema.ArgBuilder.auto.*
import caliban.schema.Annotations.GQLDescription
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ServerEndpoint.Full
import works.scala.sss.api.models.*
import works.scala.sss.api.services.TopicService
import zio.*

object TopicController:
  val layer: ZLayer[TopicService, Nothing, TopicController] = ZLayer {
    ZIO.service[TopicService].map(TopicController.apply)
  }

case class TopicController(topicService: TopicService) extends BaseController:

  private val topicsEndpoint: Endpoint[Unit, Unit, ApiError, Unit, Any] =
    baseEndpoint
      .tag("topics")
      .in("topics")

  val getTopics
      : Full[Unit, Unit, Unit, ApiError, GetTopicsResponse, Any, Task] =
    topicsEndpoint
      .name("getTopics")
      .description("Get all topics")
      .get
      .out(jsonBody[GetTopicsResponse])
      .serverLogic(_ => topicService.getTopics.handleErrors)

  val getTopic
      : Full[Unit, Unit, String, ApiError, GetTopicResponse, Any, Task] =
    topicsEndpoint
      .name("getTopic")
      .description("Get a topic by ID")
      .get
      .in(path[String]("id"))
      .out(jsonBody[GetTopicResponse])
      .serverLogic(id => topicService.getTopic(id).handleErrors)

  val createTopic: Full[
    Unit,
    Unit,
    CreateTopicRequest,
    ApiError,
    CreateTopicResponse,
    Any,
    Task
  ] =
    topicsEndpoint
      .name("createTopic")
      .description("Create a new topic")
      .post
      .in(jsonBody[CreateTopicRequest])
      .out(jsonBody[CreateTopicResponse])
      .serverLogic(in => topicService.createTopic(in).handleErrors)

  val updateTopic: Full[
    Unit,
    Unit,
    (String, UpdateTopicRequest),
    ApiError,
    UpdateTopicResponse,
    Any,
    Task
  ] =
    topicsEndpoint
      .name("updateTopic")
      .description("Update an existing topic")
      .put
      .in(path[String] / jsonBody[UpdateTopicRequest])
      .out(jsonBody[UpdateTopicResponse])
      .serverLogic((id, in) => topicService.updateTopic(id, in).handleErrors)

  val deleteTopic
      : Full[Unit, Unit, String, ApiError, DeleteTopicResponse, Any, Task] =
    topicsEndpoint
      .name("deleteTopic")
      .description("Delete an existing topic")
      .delete
      .in(path[String])
      .out(jsonBody[DeleteTopicResponse])
      .serverLogic(id => topicService.deleteTopic(id).handleErrors)

  override val routes: List[ServerEndpoint[Any, Task]] = List(
    getTopic,
    getTopics,
    createTopic,
    updateTopic,
    deleteTopic
  )
