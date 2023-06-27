package works.scala.sss.api.models

import sttp.tapir.Schema
import zio.json.JsonCodec

import java.time.Instant

case class DelayedMessage(
    topicName: String,
    sendAt: Option[Instant],
    payload: String
) derives JsonCodec,
      Schema
