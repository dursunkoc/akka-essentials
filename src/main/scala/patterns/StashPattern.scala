package patterns

import akka.actor.{Actor, ActorLogging, ActorSystem, Props, Stash}

object StashPattern extends App {

  case object Open
  case object Close
  case class Write(data:String)
  case object Read


  class Resource extends Actor with ActorLogging with Stash{
    var content = ""
    override def receive: Receive = closed

    def closed: Receive = {
      case Open =>
        log.info("Opening & unstashing")
        unstashAll()
        context.become(open)
      case _ =>
        log.info("Stashing! Not open")
        stash()
    }

    def open:Receive = {
      case Read =>
      log.info(s"Reading the $content")
        sender() ! content
      case Write(data) =>
        log info s"Writing the $data"
        content = data
      case Close =>
        log.info("Closing & unstashing")
        unstashAll()
        context.become(closed)
      case _ => log.info("Stashing! Not closed")
    }
  }

  val system = ActorSystem("stashing")
  val resource = system.actorOf(Props[Resource])

  resource ! Read
  resource ! Write("hello world!")
  resource ! Read
  resource ! Open
  resource ! Read
  resource ! Write("I Love Akka!")
  resource ! Open
  resource ! Close
  resource ! Read
}
