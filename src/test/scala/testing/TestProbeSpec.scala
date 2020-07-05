package testing

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class TestProbeSpec extends TestKit(ActorSystem("testProbeSpec"))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  import TestProbeSpec._
  "A master actor" should {

    "register a slave" in{
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")
      master ! Register(slave.ref)
      expectMsg(RegistrationAck)
    }
    "send the work to the slave actor" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")
      master ! Register(slave.ref)
      expectMsg(RegistrationAck)

      val workloadString = "I love Akka"
      master ! Work(workloadString)
      slave.expectMsg(SlaveWork(workloadString, testActor))

      slave.reply(WorkCompleted(4, testActor))
      expectMsg(Report(4))
    }

    "aggregate data correctly" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")
      master ! Register(slave.ref)
      expectMsg(RegistrationAck)

      val workloadString = "I love Akka"
      master ! Work(workloadString)
      master ! Work(workloadString)

      slave.receiveWhile(){
        case SlaveWork(w:String, testActor) => slave.reply(WorkCompleted(5, testActor))
      }

      expectMsg(Report(3))
      expectMsg(Report(6))
    }
  }
}

object TestProbeSpec{
  case class Work(text:String)
  case class SlaveWork(text:String, originalRequester:ActorRef)
  case class WorkCompleted(count:Int, originalRequester:ActorRef)
  case class Register(slaveRef:ActorRef)
  case class RegistrationAck()
  case class Report(totalCount: Int)

  class Master extends Actor{

    def online(slaveRef: ActorRef, totalWordCount:Int): Receive = {
      case Work(text) => slaveRef ! SlaveWork(text, sender())
      case WorkCompleted(count, originalRequester)=>{
        val newTotalWordCount = totalWordCount + count
        originalRequester ! Report(newTotalWordCount)
        context.become(online(slaveRef, newTotalWordCount))
      }
    }

    override def receive: Receive = {
      case Register(slaveRef) => {
        context.become(online(slaveRef, 0))
        sender() ! RegistrationAck
      }
      case _ =>
    }
  }
}

