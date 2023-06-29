package works.scala.sss.api.models

import zio.schema.Schema
import zio.schema.derived
import zio.json.JsonCodec

case class Topic(name: String) derives JsonCodec, Schema
