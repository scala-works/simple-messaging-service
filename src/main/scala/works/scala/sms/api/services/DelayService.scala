package works.scala.sms.api.services

import com.rabbitmq.client.{
  AMQP,
  Connection,
  Consumer,
  DefaultConsumer,
  Envelope,
  ShutdownSignalException
}
import works.scala.sms.api.models.DelayedMessage
import works.scala.sms.extensions.Extensions.*
import zio.{Scope, Task, ZIO, ZLayer}
import zio.json.JsonCodec
import zio.json.ast.Json
import works.scala.sms.rmq.RMQ
import zio.json.EncoderOps
import zio.json.DecoderOps

import java.time.{Duration, Instant}
import scala.jdk.CollectionConverters.*
import scala.util.{Failure, Success, Try}

trait DelayService:
  def consume: Task[Nothing]

object DelayServiceImpl:
  val layer: ZLayer[Connection, Nothing, DelayService] = ZLayer {
    ZIO.serviceWith[Connection](DelayServiceImpl.apply)
  }
case class DelayServiceImpl(rmqConnection: Connection) extends DelayService:

  private def consumeLogic = {
    rmqConnection.scopedOp { chann =>
      val consumer = new DefaultConsumer(chann) {
        override def handleDelivery(
            consumerTag: String,
            envelope: Envelope,
            properties: AMQP.BasicProperties,
            body: Array[Byte]
        ): Unit = {

          val sendAtHeader: Option[Instant] = Try {
            properties.getHeaders.asScala
              .get(RMQ.sendAtHeader)
              .map(s => Instant.parse(s.toString))
          }.toOption.flatten

          val delayTopicHeader: Option[String] = Try {
            properties.getHeaders.asScala
              .get(RMQ.delayTopicHeader)
              .map(_.toString)
          }.toOption.flatten

          delayTopicHeader match {
            case None            => {
              println(
                s"Received delayed message without ${RMQ.delayTopicHeader}!"
              )
              chann.basicAck(envelope.getDeliveryTag, false)
            }
            case Some(topicName) =>
              sendAtHeader.map(s => Duration.between(Instant.now, s)) match {
                case None           => {
                  println(
                    s"Received delayed message without ${RMQ.sendAtHeader}!"
                  )
                  chann.basicAck(envelope.getDeliveryTag, false)
                }
                case Some(duration) => {
                  val dlx         = duration match
                    case day if duration.toHours >= 24  => Some("_24hr")
                    case day if duration.toHours >= 1   => Some("_1hr")
                    case day if duration.toMinutes >= 1 => Some("_1min")
                    case _                              => Option.empty[String]
                  val destination =
                    dlx.fold(RMQ.readyExchange)(ex => RMQ.delayedExchange + ex)
                  chann.basicPublish(
                    destination,
                    s"delayed.$topicName",
                    properties,
                    body
                  )
                  chann.basicAck(envelope.getDeliveryTag, false)
                }
              }
          }
        }
      }
      chann.basicConsume(RMQ.delayedExchange, false, consumer)
    }
  }

  override def consume: Task[Nothing] =
    ZIO.scoped {
      for {
        _       <- ZIO.logInfo(s"Starting delay processor")
        _       <- consumeLogic
        nothing <- ZIO.never
      } yield nothing
    }
