package infra

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import scala.concurrent.duration._

object SelfClosingActorWithScheduler extends App {
  val system = ActorSystem("selfClosingActorWithScheduler")
  import system.dispatcher

  case object Timeout

  class SelfClosingActor extends Actor with ActorLogging{


    var closingScheduler=createClosingScheduler

    def createClosingScheduler={
      context.system.scheduler.scheduleOnce(1 second){
        self ! Timeout
      }
    }


    override def receive: Receive = {
      case Timeout  => log.warning("Closing because of timeout")
        context.stop(self)
      case msg => log.info(s"Received message ${msg}")
        closingScheduler.cancel()
        closingScheduler = createClosingScheduler
    }
  }

  val selfClosingActor: ActorRef = system.actorOf(Props[SelfClosingActor])
  selfClosingActor ! "Hello"

  val cancellable = system.scheduler.scheduleAtFixedRate(200 milli, 800 milli)(()=>{
    selfClosingActor ! "Hello again"
  })

  system.scheduler.scheduleOnce(10 second){
    system.log.info("Stopping greeting")
    cancellable.cancel()
  }


}
