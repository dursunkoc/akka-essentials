package part2Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorsIntroEx extends App {
  /*
    1. Counter actor incr decr print
    2. Bank account as an actor
    - deposit
    - withdraw
    - statement
    replies with success failure
     */

  val system = ActorSystem("actorsIntroEx")


  case class Increment(val amount:Int=1)
  case class Decrement(val amount:Int=1)
  object Print
  case class Counter(var count:Int) extends Actor{
    def receive:Receive = {
      case Increment(amount) => {
        println(s"Incrementing by ${amount}")
        this.count=this.count+amount
      }
      case Decrement(amount) => {
        println(s"Decrementing by ${amount}")
        this.count=this.count-amount
      }
      case Print => println(s"Current counter ${this.count}")
    }
  }

  val counter: ActorRef = system.actorOf(Props(new Counter(0)))
  counter ! Increment()
  counter ! Increment(3)
  counter ! Decrement(4)
  counter ! Print
}
