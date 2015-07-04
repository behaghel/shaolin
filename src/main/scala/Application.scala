package shaolin

import java.io.File
import scala.util.{Success, Failure, Properties}
import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor.{ActorSystem, Props, Actor, ActorRef}
import akka.util.Timeout
import akka.event.Logging.InfoLevel
import akka.io.IO
import akka.pattern.ask
import spray.routing.directives.LogEntry
import spray.can.Http
import spray.routing._
import spray.http._
import MediaTypes._
import StatusCodes._

object Application extends App {

  implicit val system = ActorSystem("shaolin")

  // implicit val ctx = system.dispatcher

  val service = system.actorOf(Props[ChallengerApiActor], "challenger-api")

  // $PORT is how heroku passes it
  val port = Properties.envOrElse("PORT", "8080").toInt
  IO(Http) ! Http.Bind(service, interface = "0.0.0.0", port = port)
}

class ChallengerApiActor extends Actor with ChallengerApi {
  def actorRefFactory = context
  // stolen from https://github.com/knoldus/spray-akka-starter/blob/master/src/main/scala/com/knoldus/hub/StartHub.scala
  // logs just the request method and response status at info level
  // def requestMethodAndResponseStatusAsInfo(req: HttpRequest): Any => Option[LogEntry] = {
  //   case res: HttpResponse => Some(LogEntry(req.method + ":" + req.uri + ":" + res.message.status, InfoLevel))
  //   case _ => None // other kind of responses
  // }
  // def routeWithLogging = logRequestResponse(requestMethodAndResponseStatusAsInfo _)(apiRoute)
  // def receive = runRoute(routeWithLogging)
  def receive = runRoute(apiRoute)

  def createJury(filename: String) = {
    actorRefFactory.actorOf(Jury.props(filename))
  }
}

trait ChallengerApi extends HttpService {
  implicit def executionContext = actorRefFactory.dispatcher
  implicit val timeout = Timeout(2 minutes) // whatever you need lovely sbt

  import SbtBridge._

  import Jury._
  var jurys: Map[String, ActorRef] = Map.empty
  def createJury(filename: String): ActorRef

  val logger = implicitly[spray.util.LoggingContext]
  lazy val random = new scala.util.Random()
  def genToken() = random.alphanumeric.take(6).mkString

  val tokenName = "shaolin-token"
  val apiRoute =
    get {
      pathSingleSlash {
        complete(index)
      } ~
      pathPrefix("js") { getFromResourceDirectory("js/") }
    } ~
      post {
        path("submission" / "Palindrome.java") {
          entity(as[String]) { code =>
            optionalCookie(tokenName) { tokenOpt =>
              tokenOpt match {
                case Some(token) =>
                  completeAssessment(jurys(token.content), code)
                case None =>
                  val token = genToken()
                  // TODO: cookie should expire
                  setCookie(HttpCookie(tokenName, token)) {
                    val newJury = createJury("Palindrome.java")
                    jurys += token -> newJury
                    completeAssessment(newJury, code)
                  }
              }
            }
          }
        } ~
          path("hello") {
            complete(OK)
          }
      }

  def completeAssessment(jury: ActorRef, code: String) =
    complete {
      (jury ? Assess(code)) collect {
        case SbtSuccess(Test(_)) => "Success!"
        case SbtFailure(Test(_)) => "You failed!"
        case JuryFatal(e) => e.getMessage()
      }
    }

  lazy val index =
    <html>
      <head><title>Shaolin</title></head>
      <body>
        <h1>Coding challenge</h1>
        <p>I bet you can't code even the most trivial piece of logic!</p>
      <textarea id="code" rows="20" cols="120">{"""
      // Write a function that checks if a given sentence is a
      // palindrome. A palindrome is a word, phrase, verse, or
      // sentence that reads the same backward or forward. Only the
      // order of English alphabet letters (a-z, case doesn't matter)
      // should be considered, other characters should be ignored.
      public class Palindrome {
        public static boolean isPalindrome(String str) {
          throw new UnsupportedOperationException("Leon sees Noel.");
        }
      }
      """}</textarea>
      <div><button id="submit">Submit</button></div>
      <div id="feedback"></div>
      <script src="/js/submit.js"></script>
      </body>
    </html>

  import spray.util.LoggingContext
  implicit def exceptionHandler(implicit log: LoggingContext) =
    ExceptionHandler {
      case e => ctx =>
        val err = e.getMessage()
        log.warning("error encountered while handling request {}: {}", ctx.request, err)
        ctx.complete(BadRequest, err)
    }

  // lazy val questions = new java.io.File(getClass.getResource("/questions").getFile).listFiles
}