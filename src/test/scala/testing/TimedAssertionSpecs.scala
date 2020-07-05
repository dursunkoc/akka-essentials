package testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike
import testing.TimedAssertionSpecs.{WorkResult, WorkerActor}

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class TimedAssertionSpecs extends TestKit(ActorSystem("timedAssertion"))
  with AnyWordSpecLike
  with ImplicitSender
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  "A worker actor" should{
    val worker = system.actorOf(Props[WorkerActor])
    " work " in {
      worker ! "work"
      expectMsg(590 millisecond,WorkResult(42))
    }
    "run in timely" in {
      within(500 millis, 1 second){
        worker ! "work"
        expectMsg(WorkResult(42))
      }
    }
    "complete sequence in timely" in {
      within(1 second){
        worker ! "workSequence"
        val allValues = receiveWhile(max = 130 millis, idle = 100 millis, messages = 8) {
          case WorkResult(value) => value
        }
        assert(allValues.sum > 5)
      }
    }
  }

}

object TimedAssertionSpecs{
  case class WorkResult(result:Int)

  class WorkerActor extends Actor{
    override def receive: Receive = {
      case "work" =>{
        Thread.sleep(500)
        sender() ! WorkResult(42)
      }
      case "workSequence" =>{
        val r = new Random()
        for (_ <- 1 to 10){
          Thread.sleep(r.nextInt(50))
          sender() ! WorkResult(1)
        }
      }
    }
  }
}
