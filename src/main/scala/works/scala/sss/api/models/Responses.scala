package works.scala.sss.api.models

import zio.json.JsonCodec
import zio.schema.Schema
import zio.schema.derived

case class ApiError(message: String) derives JsonCodec, Schema

case class GetTopicResponse(topic: Topic) derives JsonCodec, Schema
case class GetTopicsResponse(topics: List[Topic]) derives JsonCodec, Schema
case class CreateTopicResponse(topic: Topic) derives JsonCodec, Schema
case class UpdateTopicResponse(topic: Topic) derives JsonCodec, Schema
case class DeleteTopicResponse(topic: Topic) derives JsonCodec, Schema

case class GetSubscriptionResponse() derives JsonCodec, Schema
case class GetSubscriptionsResponse() derives JsonCodec, Schema
case class CreateSubscriptionResponse() derives JsonCodec, Schema
case class UpdateSubscriptionResponse() derives JsonCodec, Schema
case class DeleteSubscriptionResponse() derives JsonCodec, Schema

case class GetEventResponse() derives JsonCodec, Schema
case class CancelEventResponse() derives JsonCodec, Schema

case class GetSeriesResponse() derives JsonCodec, Schema
case class CancelSeriesResponse() derives JsonCodec, Schema

case class PublishMessageResponse() derives JsonCodec, Schema

case class MessageConsumeResponse(
    msgCount: Int,
    message: Option[Message]
) derives JsonCodec,
      Schema
