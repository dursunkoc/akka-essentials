package part2Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import part2Actors.ActorsBehaviourEx.Counter.{Dec, Inc, Prt}

object ActorsBehaviourEx extends App {
  val system = ActorSystem("behavior")

  object Counter {

    case object Inc

    case object Dec

    case object Prt

  }

  class Counter extends Actor {

    import Counter._

    override def receive: Receive = countReceive(0)

    def countReceive(currentCount: Int): Receive = {
      case Inc => {
        context.become(countReceive(currentCount + 1))
        println(s"Incremented from ${currentCount}")
      }
      case Dec => {
        context.become(countReceive(currentCount - 1))
        println(s"Decremented from ${currentCount}")
      }
      case Prt => println(s"Current Count is ${currentCount}")
    }
  }

  private val myCounter: ActorRef = system.actorOf(Props[Counter], "myCounter")
  myCounter ! Inc
  myCounter ! Inc
  myCounter ! Inc
  myCounter ! Dec
  myCounter ! Inc
  myCounter ! Dec
  myCounter ! Inc
  myCounter ! Dec
  myCounter ! Inc
  myCounter ! Dec
  myCounter ! Prt
  println("Done!")

  case class Vote(candidate: String)

  case object VoteStatusRequest

  case class VoteStatusReply(candidate: Option[String])

  class Citizen extends Actor {
    override def receive: Receive = vote(None)

    def vote(candidate: Option[String]): Receive = {
      case s: String => context.become(vote(Some(s)))
      case VoteStatusRequest => sender() ! candidate
    }
  }

  case class AggregateVotes(citizens: Set[ActorRef])

  class VoteAggregator extends Actor {

    override def receive: Receive = pooling

    def pooling: Receive = {
      case AggregateVotes(folk) => {
        folk.foreach(_ ! VoteStatusRequest)
        context.become(voteCounting(folk, Map()))
      }
      case _ => println("Still voting!")
    }

    def voteCounting(awaitingVotes:Set[ActorRef], currentStat:Map[String, Int]): Receive ={
      case None => {
        println(s"Still waiting from ${sender()}")
        sender() ! VoteStatusRequest
      }
      case Some(c:String) => {
        val updatedStat:Map[String, Int] = currentStat + (c -> (currentStat.getOrElse(c,0)+1))
        val updatedAwaiting = awaitingVotes-sender()
        context.become(voteCounting(updatedAwaiting, updatedStat))
        if(updatedAwaiting.isEmpty){
          println(updatedStat)
        }
      }
    }
  }

  val alice = system.actorOf(Props[Citizen],"alice")
  val bob = system.actorOf(Props[Citizen],"bob")
  val charlie = system.actorOf(Props[Citizen], "charlie")
  val jack = system.actorOf(Props[Citizen], "jack")
  val va = system.actorOf(Props[VoteAggregator])


  bob ! "Dursun"
  charlie ! "Dursun"
  jack ! "Cemil"

  va ! AggregateVotes(Set(alice, bob, charlie, jack))
  Thread.sleep(100)
  alice ! "Dursun"
}
