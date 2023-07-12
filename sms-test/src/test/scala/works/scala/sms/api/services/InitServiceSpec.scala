package works.scala.sms.api.services

import zio.*
import zio.test.*
import opinions.*
import _root_.test.opinions.*
import com.rabbitmq.http.client.Client
import works.scala.sms.rmq.RMQ
import works.scala.smstesting.RMQContainer
import works.scala.sms.extensions.Extensions.*
import zio.test.TestAspect.before

import scala.jdk.CollectionConverters.*
object InitServiceSpec extends ZIOSpecDefault:
  override def spec: Spec[TestEnvironment, Any] =
    suite("InitSpec")(
      test(
        "InitService should create the needed Exchanges, Queues, and Bindings"
      ) {
        for {
          client    <- ZIO.service[Client]
          exchanges <-
            client
              .task(_.getExchanges(RMQ.vhost))
              .someOrFail(new Exception("Unable to get list of exchanges"))
              .map(_.asScala.toList)
          queues    <-
            client
              .task(_.getQueues(RMQ.vhost))
              .someOrFail(new Exception("Unable to get list of queues"))
              .map(_.asScala.toList)
          bindings  <-
            client
              .task(_.getBindings(RMQ.vhost))
              .someOrFail(new Exception("Unable to get list of bindings"))
              .map(_.asScala.toList)
        } yield assertTrue(
          exchanges.exists(_.getName == RMQ.readyExchange),
          exchanges.exists(_.getName == RMQ.delayedExchange),
          exchanges.exists(_.getName == RMQ.delayedExchange + "_24hr"),
          exchanges.exists(_.getName == RMQ.delayedExchange + "_1hr"),
          exchanges.exists(_.getName == RMQ.delayedExchange + "_1min"),
          queues.exists(_.getName == RMQ.delayedExchange),
          queues.exists(_.getName == RMQ.delayedExchange + "_24hr"),
          queues.exists(_.getName == RMQ.delayedExchange + "_1hr"),
          queues.exists(_.getName == RMQ.delayedExchange + "_1min"),
          queues
            .filter(_.getName.startsWith(RMQ.delayedExchange + "_"))
            .forall { q =>
              val args = q.getArguments.asScala
              args
                .get("x-dead-letter-exchange")
                .contains(RMQ.delayedExchange) &&
              args.contains("x-message-ttl")
            },
          bindings
            .exists(b =>
              b.getSource == b.getDestination && b.getSource == RMQ.delayedExchange
            ),
          bindings
            .exists(b =>
              b.getSource == b.getDestination && b.getSource == RMQ.delayedExchange + "_24hr"
            ),
          bindings
            .exists(b =>
              b.getSource == b.getDestination && b.getSource == RMQ.delayedExchange + "_1hr"
            ),
          bindings
            .exists(b =>
              b.getSource == b.getDestination && b.getSource == RMQ.delayedExchange + "_1min"
            )
        )
      } @@ before(ZIO.serviceWithZIO[InitService](_.initRmq)),
      test("InitService should not fail on a system already set up") {
        for {
          _ <- ZIO.serviceWithZIO[InitService](_.initRmq)
        } yield assertCompletes
      } @@ before(ZIO.serviceWithZIO[InitService](_.initRmq))
    ).provide(
      Scope.default,
      RMQContainer.layer,
      RMQContainer.config,
      RMQ.client.zlayer,
      RMQ.connectionFactory.zlayer,
      ZLayer(RMQ.connection),
      InitServiceImpl.layer
    )
