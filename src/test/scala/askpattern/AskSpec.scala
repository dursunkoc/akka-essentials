package askpattern

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import askpattern.AskSpec.AuthenticationManager.{AuthFailure, AuthSuccess, INVALID_USER_PASS, SYSTEM_ERROR}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

class AskSpec extends TestKit(ActorSystem("askspec")) with ImplicitSender with AnyWordSpecLike {

  import AskSpec._
  import AskSpec.AuthenticationManager._

  "An Authetication manager" should {
    testAuth(Props[AuthenticationManager])
  }
  "A Piped Authetication manager" should {
    testAuth(Props[PipedAuthenticationManager])
  }

  def testAuth(props:Props): Unit ={
    "fail authentication when user does not exist" in {
      val auth = system actorOf props
      auth ! Authenticate("dursun1", "312321")
      expectMsg(AuthFailure(INVALID_USER_PASS))
    }
    "fail authentication when user exists but invalid password" in {
      val auth = system actorOf props
      auth ! Register("dursun", "1234")
      auth ! Authenticate("dursun", "4321")
      expectMsg(AuthFailure(INVALID_USER_PASS))
    }
    "success authentication when user exists and valid password" in {
      val auth = system actorOf props
      auth ! Register("dursun", "1234")
      auth ! Authenticate("dursun", "1234")
      expectMsg(AuthSuccess)
    }
  }

}

object AskSpec {

  case class Read(key: String)

  case class Write(key: String, value: String)

  class KVActor extends Actor {
    override def receive: Receive = online(Map())

    def online(registry: Map[String, String]): Receive = {
      case Read(key) => sender() ! registry.get(key)
      case Write(key, value) => context.become(online(registry + (key -> value)))
    }
  }

  object AuthenticationManager {

    case class Authenticate(user: String, password: String)

    case class Register(user: String, password: String)

    case class AuthFailure(message: String)

    case object AuthSuccess

    val INVALID_USER_PASS = "Invalid user/password"
    val SYSTEM_ERROR = "system error, try again later!"

  }

  class AuthenticationManager extends Actor {

    import AuthenticationManager._

    implicit val timeout = Timeout(1 second)
    implicit val ec = context.dispatcher
    val authDB = context.actorOf(Props[KVActor])


    override def receive: Receive = {
      case Authenticate(user, password) => handleAuth(user, password)
      case Register(user, password) => authDB ! Write(user, password)
    }

    def handleAuth(user: String, password: String) = {
      val origSender = sender()
      val fReadUser = authDB ? Read(user)
      fReadUser.onComplete {
        case Success(None) => origSender ! AuthFailure(INVALID_USER_PASS)
        case Success(Some(pass)) if pass != password => origSender ! AuthFailure(INVALID_USER_PASS)
        case Success(Some(pass)) if pass == password => origSender ! AuthSuccess
        case Failure(_) => origSender ! AuthFailure(SYSTEM_ERROR)
      }
    }
  }

  class PipedAuthenticationManager extends AuthenticationManager {
    override def handleAuth(user: String, password: String) = {
      (authDB ? Read(user))
        .mapTo[Option[String]]
        .map {
          case None => AuthFailure(INVALID_USER_PASS)
          case Some(pass) if pass != password => AuthFailure(INVALID_USER_PASS)
          case Some(pass) if pass == password => AuthSuccess
          case _ => AuthFailure(SYSTEM_ERROR)
        }
        .pipeTo(sender())
    }
  }

}
