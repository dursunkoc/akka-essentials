package part2Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorsIntro extends App {
  val system = ActorSystem("actorsIntro")

  class WordCountActor extends Actor{
    def receive:Receive = {
      case s:String => println(s"Received message ${s}")
    }
  }

  val wca = system.actorOf(Props[WordCountActor], "wcA")
  wca ! "Hello"

  class Person(val name:String) extends Actor{
    def prompt(msg:String): Unit ={
      println(s"[${self}]:[${name}]I have received ${msg} from ${sender()}")
    }
    def receive:Receive = {
      case msg:String => prompt(msg)
      case Selam(msg) => {
        prompt(msg)
        sender() ! "Aleykum Selam"
      }
      case SelamTo(msg, ref) => ref forward Selam(msg)
    }
  }
  case class Selam(message:String)
  case class SelamTo(message:String, ref:ActorRef)

  private val dursun: ActorRef = system.actorOf(Props(new Person("dursun")), "dursun")
  private val yasemin: ActorRef = system.actorOf(Props(new Person("yasemin")), "yasemin")
  private val elifNisa: ActorRef = system.actorOf(Props(new Person("elifNisa")), "elifNisa")
  private val beyza: ActorRef = system.actorOf(Props(new Person("beyza")), "beyza")

  dursun ! "hu"
  dursun.!(SelamTo("eve gel", beyza))(yasemin)
  yasemin.!(Selam("nerdesin"))(dursun)
  elifNisa.tell("eve gel!", yasemin)
}
