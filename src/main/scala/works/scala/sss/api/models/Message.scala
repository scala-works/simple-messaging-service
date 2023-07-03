package works.scala.sss.api.models

import zio.json.*
import zio.schema.Schema
import zio.schema.derived

case class Message(
    dTag: Long,
    payload: String
) derives JsonCodec,
      Schema

enum MessageConfirm derives JsonCodec, Schema:
  case Ack
  case Nack(requeue: Boolean)
  case Reject(requeue: Boolean)

case class MessageResponse(
    dTag: Long,
    confirm: MessageConfirm
) derives JsonCodec
