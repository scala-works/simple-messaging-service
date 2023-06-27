package works.scala.sss.api.services

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
import works.scala.sss.extensions.Extensions.*
import works.scala.sss.rmq.RMQ
import works.scala.sss.api.models.{Message, MessageConsumeResponse}
import zio.http.socket.{WebSocketChannelEvent, WebSocketFrame}
import zio.*
import zio.http.*
import works.scala.sss.extensions.Extensions.*
import zio.http.ChannelEvent.UserEvent.*
import zio.http.ChannelEvent.*
import zio.stream.*

import java.io.{PipedInputStream, PipedOutputStream}
import java.util.UUID
import scala.util.Try

trait ConsumerService:
  def ackingConsume(subscription: String): Task[MessageConsumeResponse]
  def handleWs(
      subscription: String,
      wsId: String
  ): PartialFunction[WebSocketChannelEvent, RIO[Any, Unit]]

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
        ref  <- Ref.make[Map[String, Channel]](Map.empty)
      } yield ConsumerServiceImpl(amqp, http, ref)
    }

case class ConsumerServiceImpl(
    rmqConnection: Connection,
    rmqClient: Client,
    refMap: Ref[Map[String, Channel]]
) extends ConsumerService:

  private def makeEntry(id: String, connection: Connection): Task[Channel] =
    for {
      channel <- ZIO.attempt(connection.createChannel())
      _       <- ZIO.attempt(channel.basicQos(1))
      _       <- refMap.getAndUpdate(_ + (id -> channel))
      _       <- ZIO.logInfo(s"Created channel $id")
    } yield channel

  private def closeEntry(id: String): ZIO[Any, Nothing, Unit] = for {
    map <- refMap.getAndUpdate(_ - id)
    _   <-
      ZIO
        .fromOption(map.get(id))
        .tap(c => ZIO.attempt(c.close()))
        .ignore
    _   <- ZIO.logInfo(s"Closed channel $id")
  } yield ()

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

  private def consumerStream(
      channel: Channel,
      subscription: String
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
      channel.basicConsume(subscription, true, consumer)
  }

  override def handleWs(
      subscription: String,
      wsId: String
  ): PartialFunction[WebSocketChannelEvent, RIO[Any, Unit]] = {
    { case ChannelEvent(ws, event) =>
      event match
        case UserEventTriggered(HandshakeComplete) =>
          for {
            channel <- makeEntry(wsId, rmqConnection)
            _       <- consumerStream(channel, subscription)
                         .tap(msg => ws.writeAndFlush(WebSocketFrame.text(msg)))
                         .runDrain
            _       <- ZIO.never
          } yield ()
        case ChannelUnregistered                   => closeEntry(wsId).unit
        case _                                     => ZIO.unit
    }
  }
