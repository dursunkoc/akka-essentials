package part2Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorsBehaviour extends App {

  object FuzzyKid{
    case object Vegetable
    case object Chocolate
    case object Accept
    case object Reject
  }

  class FuzzyKid extends Actor{
    import FuzzyKid._
    def receive: Receive = happy

    def happy:Receive = {
      case Vegetable => context.become(sad, true)
      case Chocolate => context.become(happy, true)
      case _ => sender() ! Accept
    }
    def sad:Receive = {
      case Vegetable => context.become(happy, true)
      case Chocolate => context.become(sad, true)
      case _ => sender() ! Reject
    }
  }

  object Mom{
    case class MomStart(ref:ActorRef)
  }

  class Mom extends Actor{
    import Mom._
    import FuzzyKid._
    def receive: Receive = {
      case MomStart(kid) => {
        kid ! Vegetable
        kid ! Vegetable
        kid ! Chocolate
        kid ! "Lets play"
        kid ! Vegetable
        kid ! "Lets play"
      }
      case Accept => println("Yay, my kid is happy")
      case Reject => println("My kid is sad but healthy")
    }
  }

  val system = ActorSystem("momKid")
  val mom = system.actorOf(Props[Mom], "yasemin")
  val beyza = system.actorOf(Props[FuzzyKid], "beyza")
  mom ! Mom.MomStart(beyza)

}
