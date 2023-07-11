package works.scala.sms.testing

import com.dimafeng.testcontainers.RabbitMQContainer
import zio.{Scope, ZIO, ZLayer}
import opinions.*
import works.scala.sms.config.RMQConfig

object RMQContainer {

  private def ar: ZIO[Scope, Throwable, RabbitMQContainer] =
    ZIO.acquireRelease {
      ZIO.attempt {
        val container = new RabbitMQContainer()
        container.start()
        container
      }
    } { c =>
      ZIO.attempt(c.stop()).orDie
    }

  def layer: ZLayer[Scope, Throwable, RabbitMQContainer] =
    ar.zlayer

  def config: ZLayer[RabbitMQContainer, Nothing, RMQConfig] =
    ZIO
      .serviceWith[RabbitMQContainer](c =>
        RMQConfig(
          host = c.host,
          port = c.amqpPort,
          user = c.adminUsername,
          password = c.adminPassword,
          mgmtUrl = c.httpUrl + "/api/",
          mgmtUser = c.adminUsername,
          mgmtPassword = c.adminPassword
        )
      )
      .zlayer

}
