package part2Actors

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.typesafe.config.ConfigFactory

object ActorLoggingAndConfig extends App{
  class SimpleLoggingActor extends Actor with ActorLogging{
    override def receive: Receive = {
      case s:String => log.info("Received: {}", s)
    }
  }

  val system = ActorSystem("baseSystem")
  val actor = system.actorOf(Props[SimpleLoggingActor])
  actor ! "Hello Base System"

  val config = ConfigFactory.load().getConfig("anotherConfig")
  val anotherSystem= ActorSystem("anotherBaseSystem", config)
  val anotherActor = system.actorOf(Props[SimpleLoggingActor])
  anotherActor ! "Hello Base System"

}
