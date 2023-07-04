package works.scala.sms.api.services

import com.rabbitmq.client.Connection
import com.rabbitmq.http.client.domain.QueryParameters
import com.rabbitmq.http.client.{Client, ReactorNettyClient}
import zio.*
import works.scala.sms.api.models.*
import works.scala.sms.rmq.RMQ
import works.scala.sms.extensions.Extensions.*

import scala.collection.mutable
import scala.jdk.CollectionConverters.*

trait TopicService:
  def getTopics: Task[GetTopicsResponse]
  def getTopic(id: String): Task[GetTopicResponse]
  def createTopic(in: CreateTopicRequest): Task[CreateTopicResponse]
  def updateTopic(id: String, op: UpdateTopicRequest): Task[UpdateTopicResponse]
  def deleteTopic(id: String): Task[DeleteTopicResponse]

object TopicServiceImpl:
  val layer: ZLayer[Connection & Client, Nothing, TopicService] = ZLayer {
    for {
      amqp <- ZIO.service[Connection]
      http <- ZIO.service[Client]
    } yield TopicServiceImpl(amqp, http)
      .asInstanceOf[TopicService]
  }

case class TopicServiceImpl(rmqConnection: Connection, rmqClient: Client)
    extends TopicService:
  override def getTopics: Task[GetTopicsResponse] = ZIO.attemptBlocking {
    val exchanges = rmqClient
      .getExchanges(RMQ.vhost)
      .asScala
      .iterator
      .map(i => Topic(i.getName))
      .toList

    GetTopicsResponse(exchanges)
  }

  override def getTopic(id: String): Task[GetTopicResponse] =
    ZIO.attemptBlocking {
      val info = rmqClient.getExchange(RMQ.vhost, id)
      GetTopicResponse(Topic(info.getName))
    }

  override def createTopic(in: CreateTopicRequest): Task[CreateTopicResponse] =
    // TODO check before creating?
    ZIO.scoped {
      rmqConnection.scopedOp { chan =>
        chan.exchangeDeclare(in.name, "topic", true)
        chan.exchangeBind(in.name, RMQ.readyExchange, s"delayed.${in.name}")
        CreateTopicResponse(Topic(in.name))
      }
    }

  override def updateTopic(
      id: String,
      op: UpdateTopicRequest
  ): Task[UpdateTopicResponse] = ZIO.attempt {
    // TODO probably remove update functionality...
    UpdateTopicResponse(Topic(id))
  }

  override def deleteTopic(id: String): Task[DeleteTopicResponse] =
    // TODO not allow if there are existing subscriptions
    ZIO.scoped {
      rmqConnection.scopedOp { chan =>
        chan.exchangeDelete(id)
        DeleteTopicResponse(Topic(id))
      }
    }
