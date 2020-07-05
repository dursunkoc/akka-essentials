package infra

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, Props, Terminated}
import akka.routing.{ActorRefRoutee, Broadcast, RoundRobinGroup, RoundRobinPool, RoundRobinRoutingLogic, Router}

object Routers extends App {

  //Method #1
  class Master extends Actor with ActorLogging {
    private val slaves = (1 to 5).map { i =>
      val slave = context.actorOf(Props[Slave])
      context.watch(slave)
      ActorRefRoutee(slave)
    }

    private val router = Router(RoundRobinRoutingLogic(), slaves)

    override def receive: Receive = {
      case Terminated(ref) =>
        router.removeRoutee(ref)
        val slave = context.actorOf(Props[Slave])
        context.watch(slave)
        router.addRoutee(slave)
        log.info(s"Recreated actor ${ref.path.name} as ${slave.path.name}")
      case msg => router.route(msg, sender())
    }
  }

  class Slave extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(s"Received message ${message}")
    }
  }

  def tell2Master(aMaster:ActorRef){
        import scala.concurrent.duration._
        import system.dispatcher
          system.scheduler.scheduleOnce(3 second) {
          for (i <- 1 to 15) aMaster ! s"Hello $i"
        }
  }

  val system = ActorSystem("BasicRoutingSystem")

  val master: ActorRef = system.actorOf(Props[Master], "theMaster")

  //  tell2Master(master)
  //=== === === === === === === === === === === ===
  //Method #2
  val poolMaster = system.actorOf(RoundRobinPool(5).props(Props[Slave]), "thePoolMaster")
  //  tell2Master(poolMaster)
  //=== === === === === === === === === === === ===
  //Method #3
  //GROUP router
  val slaveList = (1 to 5).map(i=>system.actorOf(Props[Slave],s"Slave-$i"))
  val pathList = slaveList.map(_.path.toString)
  val groupMaster = system.actorOf(RoundRobinGroup(pathList).props(), "theGroupMaster")
  tell2Master(groupMaster)

  groupMaster ! Broadcast("Hello everyone")
}
