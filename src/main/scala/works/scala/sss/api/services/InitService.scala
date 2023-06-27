package works.scala.sss.api.services

import com.rabbitmq.client.Connection
import com.rabbitmq.http.client.Client
import works.scala.sss.rmq.RMQ
import works.scala.sss.extensions.Extensions.*
import zio.*

import java.util
import scala.jdk.CollectionConverters.*

trait InitService:
  def initRmq: Task[Unit]

object InitServiceImpl:
  val layer: ZLayer[Client with Connection, Nothing, InitService] = ZLayer {
    for {
      amqp <- ZIO.service[Connection]
      http <- ZIO.service[Client]
    } yield InitServiceImpl(amqp, http).asInstanceOf[InitService]
  }

case class InitServiceImpl(rmqConnection: Connection, rmqClient: Client)
    extends InitService:

  private val initDly = ZIO.scoped {
    rmqConnection.scopedOp { chann =>

      // Where ready messages go
      chann.exchangeDeclare(RMQ.readyExchange, "topic", true)

      // This is where we re/process delayed messages
      chann.exchangeDeclare(RMQ.delayedExchange, "topic", true)
      chann.queueDeclare(RMQ.delayedExchange, true, false, false, null)
      chann.queueBind(RMQ.delayedExchange, RMQ.delayedExchange, "delayed.#")

      // These are the dlx buckets where messages wait to ge re/processed
      val dlxArgs: Long => util.Map[String, Any] = ttlMillis =>
        Map[String, Any](
          "x-dead-letter-exchange" -> RMQ.delayedExchange,
          "x-message-ttl"          -> ttlMillis
        ).asJava

      val buckets = Seq(
        ("_24hr", 24 * 3600000),
        ("_1hr", 1 * 3600000),
        ("_1min", 60000)
      )

      for (bucket <- buckets) {
        chann.exchangeDeclare(RMQ.delayedExchange + bucket._1, "topic", true)
        chann.queueDeclare(
          RMQ.delayedExchange + bucket._1,
          true,
          false,
          false,
          dlxArgs(bucket._2)
        )
        chann.queueBind(
          RMQ.delayedExchange + bucket._1,
          RMQ.delayedExchange + bucket._1,
          "delayed.#"
        )
      }

    }
  }

  override def initRmq: Task[Unit] =
    for {
      _ <- initDly
    } yield ()
