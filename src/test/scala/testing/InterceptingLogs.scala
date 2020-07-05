package testing

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import testing.InterceptingLogs.{Checkout, CheckoutActor}

class InterceptingLogs extends TestKit(ActorSystem("interceptingLogs", ConfigFactory.load().getConfig("interceptingLogs")))
  with AnyWordSpecLike with ImplicitSender with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  "An Order flow" should {

    val item = "simit"
    val validCreditCard = "333"
    val invalidCreditCard = "033"
    "log a fulfilled order" in {

      EventFilter.info(pattern = s"An order of $item is displaced with Order-[0-9]+", occurrences = 1) intercept {
        val checkoutActor = system.actorOf(Props[CheckoutActor])
        checkoutActor ! Checkout(item, validCreditCard)
      }
    }
    "throw error when invalid credit-card is provided" in {
      EventFilter[RuntimeException](occurrences = 1) intercept{
        val checkoutActor = system.actorOf(Props[CheckoutActor])
        checkoutActor ! Checkout(item, invalidCreditCard)
      }
    }
  }

}

object InterceptingLogs {

  case class Checkout(item: String, card: String)

  case class AuthorizeCard(card: String)

  case class PaymentAccepted()

  case class PaymentDenied()

  case class DispatchOrder(item: String)

  case object OrderConfirmed

  class CheckoutActor extends Actor with ActorLogging {
    val paymentManager: ActorRef = context.actorOf(Props[PaymentManager])
    val fulfillmentManager: ActorRef = context.actorOf(Props[FulfillmentManager])

    override def receive: Receive = awaitingCheckout

    def awaitingCheckout: Receive = {
      case Checkout(item, card) =>
        log.info("Purchasing {} with CreditCard {}", item, card)
        paymentManager ! AuthorizeCard(card)
        context.become(pendingPayment(item))

    }

    def pendingPayment(item: String): Receive = {
      case PaymentAccepted =>
        log.info("Payment for {} is accepted!", item)
        fulfillmentManager ! DispatchOrder(item)
        context.become(pendingFulfillment)

      case PaymentDenied => throw new RuntimeException("Invalid Credit card")
    }

    def pendingFulfillment: Receive = {
      case OrderConfirmed =>
        log.info("Order confirmed!")
        context.become(awaitingCheckout)
        log.info("Waiting for new checkouts.")

    }

  }

  class PaymentManager extends Actor with ActorLogging {
    override def receive: Receive = {
      case AuthorizeCard(card) =>
        if (card startsWith "0") sender() ! PaymentDenied
        else {
          Thread.sleep(4000)
          sender() ! PaymentAccepted
        }
    }
  }

  class FulfillmentManager extends Actor with ActorLogging {
    var orderId = 43

    override def receive: Receive = {
      case DispatchOrder(item) =>
        sender() ! OrderConfirmed
        orderId += 1
        log.info(s"An order of $item is displaced with Order-$orderId")
    }
  }

}
