package works.scala.sms.api.models

import zio.json.JsonCodec
import zio.schema.{Schema, derived}

import java.time.Instant

case class DelayedMessage(
    topicName: String,
    sendAt: Option[Instant],
    payload: String
) derives JsonCodec,
      Schema
