package FaultTolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props}

object Lifecycle extends App {

  case class StartChild(name: String)

  case object Fail

  class Child extends Actor with ActorLogging {
    var state = 0

    override def preStart() = log.info(s"${context.self.path.name} is starting")

    override def postStop() = log.info(s"${context.self.path.name} is stopped")

    override def preRestart(reason: Throwable, message: Option[Any]) = {
      log.info(s"${self.path.name} is about to be restarting with state $state because of ${reason.getMessage}")
    }

    override def postRestart(reason: Throwable): Unit = {
      log.info(s"${self.path.name} is restarted with state $state because of ${reason.getMessage}")
    }

    override def receive: Receive = {
      case "state" => log.info(s"${context.self.path.name} state is $state")
      case msg:String => log.info(s"${context.self.path.name} received $msg")
        state += 1
      case Fail => {
        log.warning(s"${self.path.name} is failing")
        throw new RuntimeException("I am supposed to be failing!")
      }
    }
  }

  class Parent extends Actor with ActorLogging {
    override def preStart() = log.info(s"${context.self.path.name} is starting")

    override def postStop() = log.info(s"${context.self.path.name} is stopped")

    override def receive: Receive = {
      case StartChild(name) => context.actorOf(Props[Child], name)
    }

  }

  val system = ActorSystem("lifeCycle")
  val parent: ActorRef = system.actorOf(Props[Parent], "parent")
  parent ! StartChild("child1")
  val child = system.actorSelection("/user/parent/child1")

  for(_ <- 1 to 10) child ! "Hello"
  child ! "state"
  child ! Fail
  Thread.sleep(1000)
  for(_ <- 1 to 3) child ! "Hello"
  child ! "state"
  system.log.info("Now Killing!")
  parent ! PoisonPill


}
