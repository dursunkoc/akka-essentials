package FaultTolerance

import java.io.File

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.{BackoffOpts, BackoffSupervisor}
import scala.concurrent.duration._
import scala.language.postfixOps

import scala.io.Source

object BackoffSupervisorPattern extends App {

  case object ReadFile

  class FileBasedPersistentActor extends Actor with ActorLogging {
    var datasource: Source = null

    override def preStart() = log.info(s"Starting actor ${self.path.name}")

    override def postStop() = log.info(s"Stopping actor ${self.path.name}")

    override def preRestart(reason: Throwable, message: Option[Any]) = log.info(s"Restarting actor ${self.path.name}")


    override def receive: Receive = {
      case ReadFile =>
        if (datasource == null)
          datasource = Source.fromFile(new File("src/main/resources/applications.conf"))
        log.info("Content: {}", datasource.getLines().mkString("-=-"))
    }

  }

  val backoffSuperVisor = BackoffSupervisor.props(
    BackoffOpts.onFailure(
      Props[FileBasedPersistentActor],
      "simpleActor",
      2 second,
      30 second,
      0.2)
  )
  val system = ActorSystem("readingFile")
  //  val simpleActor: ActorRef = system.actorOf(Props[FileBasedPersistentActor])
  //  simpleActor ! ReadFile
  val supervisor: ActorRef = system.actorOf(backoffSuperVisor,"simpleSupervisor")
  supervisor ! ReadFile
}
