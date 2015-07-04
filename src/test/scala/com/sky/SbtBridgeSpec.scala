package shaolin

import akka.testkit.TestProbe
import scala.concurrent.duration._
import akka.actor.{ActorSystem, Actor, Props, PoisonPill}
import akka.testkit.{TestActors, TestKit, ImplicitSender}
import org.scalatest.{WordSpecLike, Matchers, BeforeAndAfterAll}
import SbtBridge._

class SbtBridgeSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
    with WordSpecLike with Matchers with BeforeAndAfterAll {

  import _system.dispatcher

  def this() = this(ActorSystem("MySpec"))

  val scaffolder = Scaffolder("/tmp/shaolin")

  override def afterAll {
    TestKit.shutdownActorSystem(system)
    scaffolder.cleanAll()
  }

  "an SbtBridge" must {
    "initialise itself properly" in {
      val project = scaffolder.makeSimpleSbtProject("test1")
      val bridge = _system.actorOf(SbtBridge.props(project.baseDir))
      bridge ! PoisonPill
    }
    "return an SbtFailure when the project tests or compilation fail" in {
      val project = scaffolder.makeFailingSbtProject("test2")
      val bridge = _system.actorOf(SbtBridge.props(project.baseDir))
      bridge ! Test(testActor)
      expectMsg(1.minute, SbtFailure(Test(testActor)))
    }
    "return an SbtSuccess when the project tests pass" in {
      val project = scaffolder.makeSimpleSbtProject("test3")
      val bridge = _system.actorOf(SbtBridge.props(project.baseDir))
      bridge ! Test(testActor)
      expectMsg(1.minute, SbtSuccess(Test(testActor)))
    }
  }

}
