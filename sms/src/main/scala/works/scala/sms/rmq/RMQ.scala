package works.scala.sms.rmq

import zio.*
import com.rabbitmq.client.{AMQP, Channel, Connection, ConnectionFactory}
import com.rabbitmq.client.impl.AMQBasicProperties
import com.rabbitmq.http.client.{
  Client,
  ClientParameters,
  ReactorNettyClient,
  ReactorNettyClientOptions
}
import works.scala.sms.api.models.PublishMessageRequest
import works.scala.sms.config.RMQConfig
import works.scala.sms.extensions.Extensions.*

import java.time.Instant

object RMQ:

  val vhost = "ScalaWorks"

  val readyExchange   = "ready"
  val delayedExchange = "delayed"

  val sendAtHeader     = "scala-works-send-at"
  val delayTopicHeader = "scala-works-delay-topic"

  val connectionFactory: ZIO[RMQConfig, Nothing, ConnectionFactory] = for {
    config <- ZIO.service[RMQConfig]
  } yield {
    val cf = new ConnectionFactory()
    cf.setHost(config.host)
    cf.setPort(config.port)
    cf.setUsername(config.user)
    cf.setPassword(config.password)
    cf.setVirtualHost(vhost)
    cf
  }

  val connection
      : ZIO[ConnectionFactory & Client & Scope, Throwable, Connection] =
    ZIO.fromAutoCloseable[ConnectionFactory & Client, Throwable, Connection] {
      for {
        _       <- ZIO.serviceWithZIO[Client](
                     _.createVhostZIO(RMQ.vhost)
                   ) // vhost needs to exist before our connection
        factory <- ZIO.service[ConnectionFactory]
      } yield factory.newConnection()
    }

  val client: RIO[RMQConfig, Client] =
    ZIO.serviceWith[RMQConfig] { cfg =>
      new Client(
        new ClientParameters()
          .url(cfg.mgmtUrl)
          .username(cfg.user)
          .password(cfg.password)
      )
    }
