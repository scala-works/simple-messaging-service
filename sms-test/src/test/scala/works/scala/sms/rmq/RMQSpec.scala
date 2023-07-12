package works.scala.sms.rmq

import works.scala.smstesting.RMQContainer
import zio.*
import zio.test.*
import zio.test.TestAspect.{withLiveClock, withLiveEnvironment, withLiveSystem}
import works.scala.sms.extensions.Extensions.*
import works.scala.sms.rmq
import opinions.*

import scala.language.postfixOps

object RMQSpec extends ZIOSpecDefault:
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("RMQSpec")(
      test("HTTP Client can connect") {
        for {
          client <- RMQ.client
          _      <- ZIO.attemptBlocking(client.whoAmI()).debug("whoAmI")
        } yield assertCompletes
      },
      test("HTTP Client can create vhost") {
        for {
          client <- RMQ.client
          _      <- client.createVhostZIO(RMQ.vhost)
        } yield assertCompletes
      },
      test("Can connect over AMQP") {
        for {
          client <- RMQ.client
          _      <- client.createVhostZIO(RMQ.vhost)
          cf     <- RMQ.connectionFactory
          conn   <- ZIO.attempt(cf.newConnection())
          chan   <- ZIO.attempt(conn.createChannel())
          _      <- ZIO.attempt(chan.getChannelNumber).debug("getChannelNumber")
          _      <- ZIO.attempt(chan.close())
          _      <- ZIO.attempt(conn.close())
          _      <- ZIO.attemptBlocking(client.deleteVhost(RMQ.vhost))
        } yield assertCompletes
      },
      test("connection layer automatically creates vhost") {
        for {
          vh1  <- RMQ.client.map(_.getVhost(RMQ.vhost)).map(Option.apply)
          conn <- RMQ.connection
          vh2  <- RMQ.client.map(_.getVhost(RMQ.vhost)).map(Option.apply)
        } yield assertTrue(
          vh1.isEmpty,
          vh2.isDefined
        )
      },
      test("connection layer doesn't fail if vhost already exists") {
        for {
          client <- RMQ.client
          _      <- client.createVhostZIO(RMQ.vhost)
          vh1    <- RMQ.client.map(_.getVhost(RMQ.vhost)).map(Option.apply)
          conn   <- RMQ.connection
          vh2    <- RMQ.client.map(_.getVhost(RMQ.vhost)).map(Option.apply)
        } yield assertTrue(
          vh1.isDefined,
          vh2.isDefined
        )
      }
    ).provide(
      Scope.default,
      RMQContainer.layer,
      RMQContainer.config.debug,
      RMQ.client.zlayer,
      RMQ.connectionFactory.zlayer
    ) @@ withLiveEnvironment
