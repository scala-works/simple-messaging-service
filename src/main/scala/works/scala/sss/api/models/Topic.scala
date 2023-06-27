package works.scala.sss.api.models

import sttp.tapir.Schema
import zio.json.JsonCodec

case class Topic(name: String) derives JsonCodec, Schema
