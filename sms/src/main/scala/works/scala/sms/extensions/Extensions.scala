package works.scala.sms.extensions

import com.rabbitmq.client.{AMQP, BasicProperties, Channel, Connection}
import com.rabbitmq.http.client.Client
import works.scala.sms.api.models.{DelayedMessage, PublishMessageRequest}
import works.scala.sms.rmq.RMQ
import zio.json.JsonCodec
import zio.{Scope, Tag, UIO, ULayer, ZIO, ZLayer}
import zio.json.EncoderOps

import scala.jdk.CollectionConverters.*
import java.time.Instant

object Extensions:

  extension (rmqClient: Client)
    def createVhostZIO(vhost: String) =
      for {
        _   <- ZIO.logInfo(s"Initializing VirtualHost ${vhost}...")
        vhO <- ZIO.attemptBlocking {
                 Option(rmqClient.getVhost(vhost))
               }
        _   <- ZIO.when(vhO.isEmpty) {
                 ZIO.logInfo(
                   s"... VirtualHost ${vhost} not found! Creating..."
                 ) *>
                   ZIO.attemptBlocking {
                     rmqClient.createVhost(vhost)
                   } *>
                   ZIO.logInfo(s"... VirtualHost $vhost created!")
               }
        _   <- ZIO.when(vhO.isDefined)(ZIO.logInfo(s"VirtualHost $vhost exists!"))
      } yield ()

  extension (conn: Connection)
    def scopedOp[A](op: Channel => A): ZIO[Scope, Throwable, A] =
      ZIO.fromAutoCloseable(ZIO.attempt(conn.createChannel())).map(op)

  extension (chann: Channel)
    def publishMessageRequest(delayedMessage: DelayedMessage): Unit =
      val now = Instant.now()
      delayedMessage.sendAt match {
        case Some(ins) if now.isBefore(ins) => {
          val ttl   = java.time.Duration.between(now, ins)
          val dlx   = ttl match
            case day if ttl.toHours >= 24  => "_24hr"
            case day if ttl.toHours >= 1   => "_1hr"
            case day if ttl.toMinutes >= 1 => "_1min"
            case _                         => ""
          val props = new AMQP.BasicProperties.Builder()
          props.headers(
            Map[String, Any](
              RMQ.sendAtHeader     -> ins.toString,
              RMQ.delayTopicHeader -> delayedMessage.topicName
            ).asJava
          )
          chann.basicPublish(
            RMQ.delayedExchange + dlx,
            s"delayed.${delayedMessage.topicName}",
            props.build(),
            delayedMessage.payload.getBytes
          )
        }
        case _                              =>
          chann.basicPublish(
            delayedMessage.topicName,
            "",
            null,
            delayedMessage.payload.getBytes
          )
      }
