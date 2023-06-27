package works.scala.sss.rmq

import zio.*
import com.rabbitmq.client.{AMQP, Channel, Connection, ConnectionFactory}
import com.rabbitmq.client.impl.AMQBasicProperties
import com.rabbitmq.http.client.{
  Client,
  ClientParameters,
  ReactorNettyClient,
  ReactorNettyClientOptions
}
import works.scala.sss.api.models.PublishMessageRequest
import works.scala.sss.extensions.Extensions.*

import java.time.Instant

object RMQ:

  val vhost = "ScalaWorks"

  val readyExchange   = "ready"
  val delayedExchange = "delayed"

  val sendAtHeader     = "scala-works-send-at"
  val delayTopicHeader = "scala-works-delay-topic"

  case class Config(host: String, port: Int, user: String, password: String)

  val connectionFactory: ZIO[Config, Nothing, ConnectionFactory] = for {
    config <- ZIO.service[RMQ.Config]
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

  val client: Task[Client] =
    ZIO.attempt {
      new Client(
        new ClientParameters()
          .url("http://127.0.0.1:15672/api/")
          .username("guest")
          .password("guest")
      )
    }
