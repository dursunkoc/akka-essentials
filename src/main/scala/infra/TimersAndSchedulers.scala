package infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

object TimersAndSchedulers extends App {
  //simple echo actor
  //system.scheduler.scheduleOnce -- system.dispatcher
  //cancelable

  class SimpleActor extends Actor with ActorLogging{
    override def receive: Receive = {
      case msg=> log.info("Received msg: {}", msg)
    }
  }

  import scala.concurrent.duration._
  import scala.language.postfixOps

  val system = ActorSystem("scheduling")
  val simpleActorRef = system.actorOf(Props[SimpleActor])
  import system.dispatcher

  system.scheduler.scheduleOnce(1 second){
      simpleActorRef ! "Hello"
  }

  val cancellable = system.scheduler.scheduleAtFixedRate(2 second, 1 second){
    ()=>simpleActorRef ! "are you alive"
  }

  system.scheduler.scheduleOnce(10 second){
    cancellable.cancel()
  }
}
