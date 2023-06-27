package works.scala.sss.api.models

import sttp.tapir.Schema
import zio.json.JsonCodec

case class Message(
    dTag: Long,
    payload: String
) derives JsonCodec,
      Schema
