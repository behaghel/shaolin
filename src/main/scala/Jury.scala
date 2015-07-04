package shaolin

import scala.util.{Success, Failure}
import scala.concurrent.Future
import akka.actor.{Props, Actor, ActorRef}
import SbtBridge._
import FileUtils._

object Jury {
  sealed trait JuryOps
  case class Assess(code: String) extends JuryOps

  sealed trait JuryVerdicts
  case class JuryFatal(e: Throwable) extends JuryVerdicts

  def props(filename: String) = Props(new Jury(filename))
}

class Jury(filename: String) extends Actor {
  import Jury._
  implicit def executionContext = context.dispatcher
  // XXX think of having a SbtProjectPool warmed up with /n/ projects at boot time
  lazy val project = SbtProject.bootstrapTemporary()
  lazy val bridge = context.actorOf(SbtBridge.props(project.baseDir))
  // var pending: Map[ActorRef, JuryOps] = Map.empty

  def idle: Receive = {
    case op@Assess(code) =>
      val requester = context.sender()
      val prepWork =
        Future.sequence(Seq(
                          project.addFile(filename, code),
                          project.addResource(testResource)))
      prepWork onComplete {
        case Success(_) => bridge ! Test(requester)
        case Failure(e) => requester ! JuryFatal(e)
      }
      bridge ! Test
  }
  def receive = idle
  // def assessing: Receive = {
  // }

  // on stop project.erase()

  val questionsRoot = "/questions/"
  lazy val testResource = questionsRoot+source2test(filename)
}