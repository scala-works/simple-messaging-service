package works.scala.sms.api.services

import zio.*
import com.rabbitmq.client.{
  AMQP,
  Channel,
  Connection,
  DefaultConsumer,
  DeliverCallback,
  Delivery,
  Envelope
}
import com.rabbitmq.http.client.Client
import works.scala.sms.extensions.Extensions.*
import works.scala.sms.rmq.RMQ
import works.scala.sms.api.models.*
import works.scala.sms.api.models.MessageConfirm.*
import works.scala.sms.config.RMQConfig
import zio.*
import zio.http.*
import works.scala.sms.extensions.Extensions.*
import zio.http.ChannelEvent.UserEvent.*
import zio.http.ChannelEvent.*
import zio.stream.*
import zio.json.*

import java.io.{PipedInputStream, PipedOutputStream}
import java.util.UUID
import scala.util.Try
import zio.http.WebSocketFrame.Close
import opinions.*

trait ConsumerService:
  def ackingConsume(subscription: String): Task[MessageConsumeResponse]
  def handleWs(
      subscription: String,
      preFetch: Int,
      autoAck: Boolean
  ): Handler[Any, Throwable, WebSocketChannel, Unit]

object ConsumerServiceImpl:
  val layer: ZLayer[
    Client & Connection,
    Throwable,
    ConsumerService
  ] =
    ZLayer {
      for {
        amqp <- ZIO.service[Connection]
        http <- ZIO.service[Client]
      } yield ConsumerServiceImpl(amqp, http)
    }

case class ConsumerServiceImpl(
    rmqConnection: Connection,
    rmqClient: Client
) extends ConsumerService:

  private def consumerStream(
      channel: Channel,
      subscription: String,
      autoAck: Boolean
  ): ZStream[Any, Throwable, String] = ZStream.async[Any, Throwable, String] {
    cb =>
      val consumer = new DefaultConsumer(channel) {
        override def handleDelivery(
            consumerTag: String,
            envelope: Envelope,
            properties: AMQP.BasicProperties,
            body: Array[Byte]
        ): Unit =
          cb(
            Chunk(new String(body)).uio
          )
      }
      channel.basicConsume(subscription, autoAck, consumer)
  }

  override def handleWs(
      subscription: String,
      preFetch: Int,
      autoAck: Boolean
  ): Handler[Any, Throwable, WebSocketChannel, Unit] =
    Handler.webSocket { ws =>
      ZIO
        .scoped {
          for {
            connection <- RMQ.connection
            channel    <- ZIO.attempt(connection.createChannel())
            _          <- ZIO.attempt(channel.basicQos(preFetch))
            _          <- ws.receiveAll {
                            case UserEventTriggered(HandshakeComplete) =>
                              consumerStream(channel, subscription, autoAck)
                                .tap(msg => ws.send(Read(WebSocketFrame.Text(msg))))
                                .runDrain
                                .fork
                            case Read(WebSocketFrame.Text(msg))        =>
                              ZIO
                                .fromEither(msg.fromJson[MessageResponse])
                                .flatMap { msg =>
                                  ZIO.whenCase(msg.confirm) {
                                    case MessageConfirm.Ack             =>
                                      ZIO.attempt(channel.basicAck(msg.dTag, false))
                                    case MessageConfirm.Nack(requeue)   =>
                                      ZIO.attempt(
                                        channel.basicNack(msg.dTag, false, requeue)
                                      )
                                    case MessageConfirm.Reject(requeue) =>
                                      ZIO.attempt(channel.basicReject(msg.dTag, requeue))
                                  }
                                }
                                .when(!autoAck)
                                .ignore
                            case Unregistered                          => ZIO.interrupt
                            case Read(Close(_, _))                     => ZIO.interrupt
                            case _                                     => ZIO.unit
                          }
            _          <- ZIO.unit
          } yield ()
        }
        .provide(
          ZLayer(RMQ.client),
          ZLayer(RMQ.connectionFactory),
          ConfigLayer[RMQConfig]("works.scala.sms.rmq")
        )
    }

  override def ackingConsume(
      subscription: String
  ): Task[MessageConsumeResponse] =
    ZIO
      .scoped {
        rmqConnection.scopedOp { chann =>
          val response =
            Try(Option(chann.basicGet(subscription, true))).toOption.flatten
          MessageConsumeResponse(
            msgCount = response.map(_.getMessageCount).getOrElse(0),
            message = response.map { r =>
              Message(
                dTag = r.getEnvelope.getDeliveryTag,
                payload = new String(r.getBody)
              )
            }
          )
        }
      }
      .logError("ackingConsume")
