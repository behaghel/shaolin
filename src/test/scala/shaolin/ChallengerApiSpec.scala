package shaolin

import org.scalatest._
import spray.http.HttpEntity
import spray.testkit.ScalatestRouteTest
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import spray.http.HttpRequest
import spray.http.HttpMethods._
import spray.http.StatusCodes._
import spray.routing.HttpService
import SbtBridge._
import Jury._

class ChallengerApiSpec extends FlatSpec with Matchers with ScalatestRouteTest
    with ChallengerApi {

  def actorRefFactory = system // connect the DSL to the test ActorSystem
  class DummyJury extends Actor {
    def receive = {
      case Assess("success") => SbtSuccess(Test(context.sender()))
      case Assess("fail") => SbtFailure(Test(context.sender()))
      case _ => JuryFatal(new RuntimeException("oops"))
    }
  }
  def createJury(filename: String): ActorRef =
    actorRefFactory.actorOf(Props(new DummyJury()))

  "shaolin.ChallengerApi" should "serve the index page on /" in {
    Get() ~> apiRoute ~> check {
      responseAs[String] should include ("Shaolin")
    }
  }
  it should "serve js assets" in {
    Get("/js/submit.js") ~> apiRoute ~> check {
      handled should be (true)
    }
  }
  it should "accept code submission and set a cookie" in {
    // val req = HttpRequest(POST, "http://localhost/submission/Palindrome.java",
    //             entity = HttpEntity("my code"))
    // val req = HttpRequest(POST, "/submission/Palindrome.java",
    //             entity = HttpEntity("my code"))
    // val req = Post("/hello", "my code")
    val req = Post("/submission/Palindrome.java", "my code")
    pendingUntilFixed {
      req ~> apiRoute ~> check {
        // it seems that because the route complete with a Future, this test fails…
        handled should be (true)
        header("Set-Cookie") should not be empty
        // responseAs[String] === "oops"
      }
    }
  }
}
