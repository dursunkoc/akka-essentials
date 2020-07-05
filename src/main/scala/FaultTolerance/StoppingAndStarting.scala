package FaultTolerance

import FaultTolerance.StoppingAndStarting.Parent.{StartChild, StopChild}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props, Terminated}

object StoppingAndStarting extends App {

  object Parent {
    case class StartChild(name: String)
    case class StopChild(name: String)
    case object Stop
  }

  class Parent extends Actor {

    import Parent._

    override def receive: Receive = withChildren(Map[String, ActorRef]())

    def withChildren(children: Map[String, ActorRef]): Receive = {
      case StartChild(name) =>
        val child = context.actorOf(Props[Child], name)
        context.become(withChildren(children + (name -> child)))
      case StopChild(name) =>
        val child: Option[ActorRef] = children.get(name)
        child.foreach(context.stop(_))
        context.become(withChildren(children - name))
      case Stop =>
        context.stop(self)
    }
  }

  class Child extends Actor with ActorLogging{
    override def receive: Receive = {
      case msg: String => log.info(s"${self.path.name.toUpperCase()} Received: $msg")
    }
  }

  import Parent._
  val system = ActorSystem("stopStart")

  def experimentContextStop = {
    val parent: ActorRef = system.actorOf(Props[Parent], "parent")
    parent ! StartChild("child1")
    parent ! StartChild("child2")
    val child1 = system.actorSelection("/user/parent/child1")
    child1 ! "Hello 1"
    parent ! StopChild("child1")
    for (_ <- 1 to 10)
      child1 ! "HellO 1"

    val child2 = system.actorSelection("/user/parent/child2")
    child2 ! "Hello 2"
    parent ! Stop
    for (_ <- 1 to 100)
      child2 ! "HellO 2"
  }

 def experimentPoisonPillAndKill: Unit ={
   val toBePoisoned = system.actorOf(Props[Child], "toBePoisoned")
   val toBeKilled = system.actorOf(Props[Child], "toBeKilled")
   toBePoisoned ! "Are you alive?"
   toBePoisoned ! PoisonPill
   for (_<-1 to 10)
     toBePoisoned ! "Are you alive?"


   toBeKilled ! "Are you alive?"
   toBeKilled ! Kill
   for (_<-1 to 10)
     toBeKilled ! "Are you alive?"
 }


  def experimentDeadWatch: Unit ={
    class WatchingParent extends Actor{
      override def receive: Receive = withChildren(Map())

      def withChildren(children:Map[String, ActorRef]):Receive ={
        case StartChild(name) =>
          val child = context.actorOf(Props[Child], name)
          context.watch(child)
          context.become(withChildren(children + (name->child)))
        case Terminated(ref) =>
          val name = ref.path.name
          context.become(withChildren(children - name))
        case "list" =>
          println(children)
      }
    }
    val parent = system.actorOf(Props[WatchingParent], "parent")
    parent ! StartChild("child1")
    parent ! StartChild("child2")
    parent ! StartChild("child3")
    parent ! "list"
    Thread.sleep(200)

    val child1 = system.actorSelection("/user/parent/child1")
    child1 ! Kill
    Thread.sleep(200)
    parent ! "list"

    val child2 = system.actorSelection("/user/parent/child2")
    child2 ! Kill
    Thread.sleep(200)
    parent ! "list"


  }
  experimentDeadWatch
}
