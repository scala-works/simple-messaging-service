package works.scala.sss.api.models

import zio.json.*
import sttp.tapir.Schema

import java.time.Instant

case class GetTopicRequest() derives JsonCodec, Schema

case class CreateTopicRequest(
    name: String
) derives JsonCodec,
      Schema

case class UpdateTopicRequest() derives JsonCodec, Schema
case class DeleteTopicRequest() derives JsonCodec, Schema

case class GetSubscriptionRequest() derives JsonCodec, Schema
case class GetSubscriptionsRequest() derives JsonCodec, Schema
case class CreateSubscriptionRequest(name: String, topic: String)
    derives JsonCodec,
      Schema
case class UpdateSubscriptionRequest() derives JsonCodec, Schema
case class DeleteSubscriptionRequest(name: String) derives JsonCodec, Schema

case class GetEventRequest() derives JsonCodec, Schema
case class CancelEventRequest() derives JsonCodec, Schema

case class GetSeriesRequest() derives JsonCodec, Schema
case class CancelSeriesRequest() derives JsonCodec, Schema

case class PublishMessageRequest(
    sendAt: Option[Instant],
    payload: String
) derives JsonCodec,
      Schema
