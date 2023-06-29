package works.scala.sss.api.models

import zio.json.JsonCodec
import zio.schema.Schema
import zio.schema.derived

case class Message(
    dTag: Long,
    payload: String
) derives JsonCodec,
      Schema
