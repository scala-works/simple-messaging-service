package works.scala.sms.config

import com.typesafe.config.ConfigFactory
import zio.config.magnolia.{Descriptor, descriptor}
import zio.config.{ReadError, toKebabCase}
import zio.config.typesafe.TypesafeConfig
import zio.*

object ConfigLoader:

  def layer[C: Descriptor: Tag](
      path: String
  ): Layer[ReadError[String], C] =
    implicit lazy val configDescriptor: _root_.zio.config.ConfigDescriptor[C] =
      descriptor[C].mapKey(toKebabCase)
    TypesafeConfig
      .fromTypesafeConfig(
        ZIO
          .attempt(ConfigFactory.load().getConfig(path)),
        configDescriptor
      )
