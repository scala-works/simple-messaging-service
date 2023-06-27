package works.scala.sss.api.controllers

import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody
import works.scala.sss.api.models.{
  CreateSubscriptionRequest,
  CreateSubscriptionResponse,
  DeleteSubscriptionResponse,
  GetSubscriptionResponse,
  GetSubscriptionsResponse
}
import works.scala.sss.api.services.SubscriptionService
import zio.*
import sttp.tapir.server.ServerEndpoint

object SubscriptionController:
  val layer: ZLayer[SubscriptionService, Nothing, SubscriptionController] =
    ZLayer {
      ZIO.service[SubscriptionService].map(SubscriptionController.apply)
    }

case class SubscriptionController(subscriptionService: SubscriptionService)
    extends BaseController:

  private val subEndpoint =
    baseEndpoint
      .tag("subscriptions")
      .in("subscriptions")

  val createSubscription =
    subEndpoint
      .name("createSubscription")
      .description("Create a new subscription")
      .post
      .in(jsonBody[CreateSubscriptionRequest])
      .out(jsonBody[CreateSubscriptionResponse])
      .serverLogic(in =>
        subscriptionService.createSubscription(in).handleErrors
      )

  val getSubscription =
    subEndpoint
      .name("getSubscription")
      .description("Get information about a subscription")
      .in(path[String]("id"))
      .out(jsonBody[GetSubscriptionResponse])
      .serverLogic(id => subscriptionService.getSubscription(id).handleErrors)

  val getSubscriptions =
    subEndpoint
      .name("getSubscriptions")
      .description("Get all subscriptions")
      .out(jsonBody[GetSubscriptionsResponse])
      .serverLogic(_ => subscriptionService.getSubscriptions().handleErrors)

  val deleteSubscription =
    subEndpoint
      .name("deleteSubscription")
      .description("Delete a subscription")
      .in(path[String]("name"))
      .out(jsonBody[DeleteSubscriptionResponse])
      .serverLogic(name =>
        subscriptionService.deleteSubscription(name).handleErrors
      )

  override val routes: List[ServerEndpoint[Any, Task]] = List(
    createSubscription,
    getSubscription,
    getSubscriptions,
    deleteSubscription
  )
