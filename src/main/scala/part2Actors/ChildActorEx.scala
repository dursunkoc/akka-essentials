package part2Actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActorEx extends App{
  object WordCounterMaster{
    case class Initialize(nChildren:Int)
    case class WordCountTask(taskId:Int, text:String)
    case class WordCountReply(taskId:Int, count:Int)
  }
  class WordCounterMaster extends Actor{
    import WordCounterMaster._
    def prompt(msg:String): Unit = {
      println(s"[master(${Thread.currentThread().getName})]: ${msg}")
    }

    override def receive: Receive = {
      case Initialize(n) => {
        prompt(s"Initialized with $n children")
        val workers = (1 to n).map(id => context.actorOf(Props(new WordCounterWorker(id)), s"worker_$id"))
        context.become(working(workers, 0, 0, Map()))
      }
    }

    def working(workers:Seq[ActorRef], workerIndex:Int, taskId:Int, taskRegistry:Map[Int, ActorRef]):Receive={
      case text:String => {
        prompt(s"Received task with text: ${text}")
        workers(workerIndex) ! WordCountTask(taskId, text)
        val nextTaskRegistry = taskRegistry + (taskId -> sender())
        val nextWorkerIndex = (workerIndex + 1) % workers.length
        val nextTaskId = taskId + 1
        context.become(working(workers, nextWorkerIndex, nextTaskId, nextTaskRegistry))
      }
      case WordCountReply(taskId, count) =>{
        prompt(s"Task $taskId completed: $count")
        taskRegistry.get(taskId).get ! count
        val nextTaskRegistry = taskRegistry - taskId
        context.become(working(workers, workerIndex, taskId, nextTaskRegistry))
      }
    }
  }

  class WordCounterWorker(val id:Int) extends Actor{
    import WordCounterMaster._
    def prompt(msg:String): Unit = {
      println(s"[worker($id)(${Thread.currentThread().getName})]: ${msg}")
    }

    override def receive: Receive = {
      case WordCountTask(id, text) => {
        prompt(s"Working on Task$id: ($text)")
        sender() ! WordCountReply(id, text.split(" ").length)
      }
    }
  }

  class Boss extends Actor{
    import WordCounterMaster._
    def prompt(msg:String): Unit = {
      println(s"[boss(${Thread.currentThread().getName})]: ${msg}")
    }
    override def receive: Receive = {
      case "go" =>{
        val master = context.actorOf(Props[WordCounterMaster], "master")
        master ! Initialize(3)
        master ! "Hello"
        master ! "Hello Hello"
        master ! "Hello Hello Hello"
        master ! "Hello Hello Hello Hello"
        master ! "Hello Hello Hello Hello Hello"
      }
      case n:Int =>{
        prompt(s"Result $n")
      }
    }
  }

  private val system: ActorSystem = ActorSystem("childActors")
  private val boss: ActorRef = system.actorOf(Props[Boss], "boss")
  boss ! "go"
}
