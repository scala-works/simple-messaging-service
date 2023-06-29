import zio.*
import works.scala.sss.api.controllers.*
import caliban.parsing.adt.OperationType.Subscription
import zio.http.*
import sttp.tapir.server.ziohttp.ZioHttpInterpreter
import works.scala.sss.api.services.*
import works.scala.sss.rmq.RMQ
import works.scala.sss.extensions.Extensions.*

object Main extends ZIOAppDefault:

  val makeRoutes = for {
    topics        <- ZIO.service[TopicController]
    subscriptions <- ZIO.service[SubscriptionController]
    publishing    <- ZIO.service[PublishingController]
    messages      <- ZIO.service[MessageController]
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
    msg    <- ZIO.service[MessageController]
    _      <- ZIO.logInfo("Server started: http://localhost:9000/docs")
    _      <- Server.install(
                routes.reduce(_ ++ _).toApp // TODO not use reduce
              )
    _      <- ZIO.never
  } yield ExitCode.success

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    program
      .provide(
        Server.Config.default.port(9000).ulayer,
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
        RMQ.Config("localhost", 5672, "guest", "guest").ulayer,
        InitServiceImpl.layer,
        DelayServiceImpl.layer,
        ConsumerServiceImpl.layer,
        MessageController.layer
      )
