package infra

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

import scala.concurrent.Future
import scala.util.Random

object Dispatchers extends App {

  class Counter extends Actor with ActorLogging {
    var count = 0

    override def receive: Receive = {
      case msg =>
        count += 1
        log.info(s"[${count}] ${msg}")
    }
  }

  val system = ActorSystem("theDispatcher")
  val counters = (1 to 10).map(c =>
    system.actorOf(Props[Counter].withDispatcher("my-dispatcher"), s"counter_$c"))


  val r = Random
  //  for (i <- 1 to 100) {
  //    counters(r.nextInt(10)) ! s"message - $i"
  //  }

  val rtjvm = system.actorOf(Props[Counter], "rtjvm")
//  for (i <- 1 to 100) {
//    rtjvm ! s"message - $i"
//  }

  class AsyncMessageActor extends Actor with ActorLogging{
    implicit val ec = system.dispatcher//s.lookup("f-dispatcher")
    override def receive: Receive = {
      case msg =>
        log.info(s"Handling $msg!")
        Future{
        log.info(s"Saving $msg!")
        Thread.sleep(5000)
        log.info(s"Successfully saved $msg!")
      }
    }
  }

  val asyncAct = (1 to 10).map(c =>
    system.actorOf(Props[AsyncMessageActor], s"asyn_$c"))
    for (i <- 1 to 100) {
      asyncAct(r.nextInt(10)) ! s"message - $i"
    }
}
