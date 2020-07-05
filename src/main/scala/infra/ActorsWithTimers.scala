package infra


import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Timers}

import scala.concurrent.duration._

object ActorsWithTimers extends App {

  case object Start
  case object Stop
  case object NextTick
  case object HeartBeatKey

  class TimerBasedHeartbeatActor extends Actor with ActorLogging with Timers{
    timers.startSingleTimer(HeartBeatKey, Start, 1 second)
    override def receive: Receive = {
      case Start => log.info(s"Bootstrapping")
        timers.startTimerAtFixedRate(HeartBeatKey, NextTick, 300 milli)
      case NextTick => log.info("I am alive")
      case Stop => timers.cancel(HeartBeatKey)
      log.info("Closing the timer!")
    }
  }

  val system = ActorSystem("timerHeartBeat")
  val tmHA: ActorRef = system.actorOf(Props[TimerBasedHeartbeatActor])
  import system.dispatcher

  system.scheduler.scheduleOnce(5 second){
    tmHA ! Stop
  }


}
