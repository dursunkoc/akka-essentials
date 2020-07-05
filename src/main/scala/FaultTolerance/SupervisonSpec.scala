package FaultTolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, Props, Terminated}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class SupervisionSpec extends TestKit(ActorSystem("superVision"))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {

  import SupervisionSpec._

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }


  "A supervisor" should{

    "process a valid input" in{
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[WordCounter]
      val wordCounterRef = expectMsgType[ActorRef]
      wordCounterRef ! "Hello this is dursun"
      wordCounterRef ! Report
      expectMsg(4)
    }
    "resume processing incase of an null pointer exception" in{
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[WordCounter]
      val wordCounterRef = expectMsgType[ActorRef]
      wordCounterRef ! "Hello this is dursun"
      wordCounterRef ! Report
      expectMsg(4)

      EventFilter[NullPointerException] () intercept{
        wordCounterRef ! ""
      }

      wordCounterRef ! Report
      expectMsg(4)
    }
    "stop incase of a runtime exception" in{
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[WordCounter]
      val wordCounterRef = expectMsgType[ActorRef]
      wordCounterRef ! "Hello this is dursun"
      wordCounterRef ! Report
      expectMsg(4)
      watch(wordCounterRef)

      EventFilter[RuntimeException] () intercept{
        wordCounterRef ! "s" * 21
      }
      val terminated = expectMsgType[Terminated]
      assert(terminated.actor == wordCounterRef)

    }
  }
}

object SupervisionSpec{

  case object Report

  class Supervisor extends Actor{
    override val supervisorStrategy = OneForOneStrategy(){
      case _:IllegalStateException => Restart
      case _:NullPointerException => Resume
      case _:RuntimeException => Stop
      case _:Exception => Escalate
    }

    override def receive: Receive = {
      case props:Props => sender() ! context.actorOf(props, "child")
    }
  }

  class WordCounter extends Actor{
    var counter = 0
    override def receive: Receive = {
      case Report => sender() ! counter
      case "" => throw new NullPointerException("Sentence is empty.")
      case str:String if str.length > 20 => throw new RuntimeException("Sentence is too long.")
      case str:String if !str(0).isUpper => throw new IllegalStateException("Invalid sentence.")
      case str:String => counter += str.split(" ").length
      case _ => throw new Exception("I can process only strings.")
    }
  }
}

