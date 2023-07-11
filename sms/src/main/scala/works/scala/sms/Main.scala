import zio.*
import works.scala.sms.api.controllers.*
import caliban.parsing.adt.OperationType.Subscription
import zio.http.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import works.scala.sms.api.services.*
import works.scala.sms.config.{ConfigLoader, RMQConfig, ServerConfig}
import works.scala.sms.rmq.RMQ
import works.scala.sms.extensions.Extensions.*

object Main extends ZIOAppDefault:

  val makeRoutes = for {
    topics        <- ZIO.service[TopicController]
    subscriptions <- ZIO.service[SubscriptionController]
    publishing    <- ZIO.service[PublishingController]
    messages      <- ZIO.service[ConsumerController]
  } yield {
    val combined = List(topics, subscriptions, publishing, messages)
      .flatMap(_.routes)
    combined
  }

  val program = for {
    _      <- ZIO.serviceWithZIO[InitService](_.initRmq)
    _      <-
      ZIO.serviceWithZIO[DelayService](_.consume).resurrect.ignore.forever.fork
    routes <- makeRoutes
    msg    <- ZIO.service[ConsumerController]
    cfg    <- ZIO.service[ServerConfig]
    _      <- ZIO.logInfo(s"Server started: http://localhost:${cfg.port}")
    _      <- Server.install(
                routes.reduce(_ ++ _).toApp ++ msg.socketApp
              )
    _      <- ZIO.never
  } yield ExitCode.success

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    program
      .provide(
        ConfigLoader.layer[ServerConfig]("works.scala.sms.server"),
        ZLayer
          .service[ServerConfig]
          .flatMap(env => Server.Config.default.port(env.get.port).ulayer),
        Server.live,
        TopicController.layer,
        SubscriptionController.layer,
        PublishingController.layer,
        TopicServiceImpl.layer,
        SubscriptionServiceImpl.layer,
        PublishingServiceImpl.layer,
        ZLayer(RMQ.client),
        ZLayer(RMQ.connectionFactory),
        ZLayer.scoped {
          RMQ.connection
        },
        ConfigLoader.layer[RMQConfig]("works.scala.sms.rmq"),
        InitServiceImpl.layer,
        DelayServiceImpl.layer,
        ConsumerServiceImpl.layer,
        ConsumerController.layer
      )
