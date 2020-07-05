package infra

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}
import akka.actor.ActorSystem.Settings
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import com.typesafe.config.Config

object Mailboxes extends App {

  class SimpleActor extends Actor with ActorLogging{
    override def receive: Receive = {
      case msg => log.info(s"Received $msg")
    }
  }

  class SupportTicketPriorityMailbox (settings:Settings, config:Config) extends UnboundedPriorityMailbox(
   PriorityGenerator{
     case msg:String if msg startsWith "[P0]"=>0
     case msg:String if msg startsWith "[P1]"=>1
     case msg:String if msg startsWith "[P2]"=>2
     case msg:String if msg startsWith "[P3]"=>3
     case _=>4
   }
  )

  val system = ActorSystem("mailboxes")
  val simpleActor = system.actorOf(Props[SimpleActor].withDispatcher("support-ticket-dispatcher"))
  simpleActor ! PoisonPill
  simpleActor ! "[P5] msg1"
  simpleActor ! "[P4] msg1"
  simpleActor ! "[P2] msg1"
  simpleActor ! "[P1] msg1"
  simpleActor ! "[P3] msg1"
  simpleActor ! "[P0] msg1"

}
