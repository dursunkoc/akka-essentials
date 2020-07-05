package testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import scala.language.postfixOps


class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  import BasicSpec._

  "An echo actor" should {
    "send back the same message" in {
      val theEcho = system.actorOf(Props[EchoActor], "theEcho")
      theEcho ! "Hello"
      expectMsg("Hello")
    }
  }

  "A blackhole actor" should{
    "not return any message" in{
      val theBlackHole = system.actorOf(Props[BlackHoleActor], "theBlackHole")
      theBlackHole ! "Hello"
      expectNoMessage(1 second)
    }
  }


}

object BasicSpec {

  class EchoActor extends Actor {
    override def receive: Receive = {
      case msg => sender() ! msg
    }
  }

  class BlackHoleActor extends Actor {
    override def receive: Receive = {
      case msg =>
    }
  }
}