package works.scala.sms.api.controllers

import works.scala.sms.api.models.*
import works.scala.sms.api.services.SubscriptionService
import zio.*
import zio.http.*
import zio.http.codec.HttpCodec
import zio.http.endpoint.*

object SubscriptionController:
  val layer: ZLayer[SubscriptionService, Nothing, SubscriptionController] =
    ZLayer {
      ZIO.service[SubscriptionService].map(SubscriptionController.apply)
    }

case class SubscriptionController(subscriptionService: SubscriptionService)
    extends BaseController:
  import HttpCodec._

  val createSubscription =
    Endpoint
      .post("subscriptions")
      .in[CreateSubscriptionRequest]
      .out[CreateSubscriptionResponse]
      .outError[ApiError](Status.InternalServerError)
      .implement(in => subscriptionService.createSubscription(in).handleErrors)

  val getSubscription =
    Endpoint
      .get("subscriptions" / string("name"))
      .out[GetSubscriptionResponse]
      .outError[ApiError](Status.InternalServerError)
      .implement(name => subscriptionService.getSubscription(name).handleErrors)

  val getSubscriptions =
    Endpoint
      .get("subscriptions")
      .out[GetSubscriptionsResponse]
      .outError[ApiError](Status.InternalServerError)
      .implement(name => subscriptionService.getSubscriptions().handleErrors)

  val deleteSubscription =
    Endpoint
      .delete("subscriptions" / string("name"))
      .out[DeleteSubscriptionResponse]
      .outError[ApiError](Status.InternalServerError)
      .implement(name =>
        subscriptionService.deleteSubscription(name).handleErrors
      )

  override val routes: List[Routes[Any, ApiError, EndpointMiddleware.None]] =
    List(
      getSubscription,
      getSubscriptions,
      createSubscription,
      deleteSubscription
    )
