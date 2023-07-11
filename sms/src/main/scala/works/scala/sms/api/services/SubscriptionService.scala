package works.scala.sms.api.services

import com.rabbitmq.client.Connection
import com.rabbitmq.http.client.Client
import zio.*
import works.scala.sms.api.models.*
import works.scala.sms.rmq.RMQ
import works.scala.sms.extensions.Extensions.*
import scala.jdk.CollectionConverters.*

trait SubscriptionService:
  def createSubscription(
      createSubscriptionRequest: CreateSubscriptionRequest
  ): Task[CreateSubscriptionResponse]

  def getSubscription(id: String): Task[GetSubscriptionResponse]

  def getSubscriptions(
  ): Task[GetSubscriptionsResponse]

  def updateDescription(
      updateSubscriptionRequest: UpdateSubscriptionRequest
  ): Task[UpdateSubscriptionResponse]

  def deleteSubscription(
      name: String
  ): Task[DeleteSubscriptionResponse]

object SubscriptionServiceImpl:
  val layer: ZLayer[Connection & Client, Nothing, SubscriptionService] =
    ZLayer {
      for {
        amqp <- ZIO.service[Connection]
        http <- ZIO.service[Client]
      } yield SubscriptionServiceImpl(amqp, http)
        .asInstanceOf[SubscriptionService]
    }

case class SubscriptionServiceImpl(rmqConnection: Connection, rmqClient: Client)
    extends SubscriptionService:
  override def createSubscription(
      createSubscriptionRequest: CreateSubscriptionRequest
  ): Task[CreateSubscriptionResponse] =
    ZIO.scoped {
      rmqConnection.scopedOp { chan =>
        chan.queueDeclare(
          createSubscriptionRequest.name,
          true,
          false,
          false,
          null
        )
        chan.queueBind(
          createSubscriptionRequest.name,
          createSubscriptionRequest.topic,
          "#",
          null
        )
        CreateSubscriptionResponse()
      }
    }

  override def getSubscription(id: String): Task[GetSubscriptionResponse] =
    ZIO.attemptBlocking {
      val queueInfo = rmqClient.getQueue(RMQ.vhost, id)
      GetSubscriptionResponse()
    }

  override def getSubscriptions(
  ): Task[GetSubscriptionsResponse] =
    ZIO.attemptBlocking {
      val queues =
        rmqClient.getQueues(RMQ.vhost).asScala.iterator.map(identity).toSeq
      GetSubscriptionsResponse()
    }

  override def updateDescription(
      updateSubscriptionRequest: UpdateSubscriptionRequest
  ): Task[UpdateSubscriptionResponse] =
    ZIO.scoped {
      rmqConnection.scopedOp { chan =>
        // TODO
        UpdateSubscriptionResponse()
      }
    }
  override def deleteSubscription(
      name: String
  ): Task[DeleteSubscriptionResponse] =
    ZIO.scoped {
      rmqConnection.scopedOp { chan =>
        chan.queueDelete(name)
        DeleteSubscriptionResponse()
      }
    }
