package shaolin

import java.io.File
import scala.util.Failure
import scala.util.Properties
import scala.concurrent.duration._
import akka.actor.{ActorSystem, Props, Actor, ActorRef}
import akka.util.Timeout
import akka.event.Logging.InfoLevel
import akka.io.IO
import akka.pattern.ask
import scala.util.Success
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
}

trait ChallengerApi extends HttpService {
  implicit def executionContext = actorRefFactory.dispatcher

  val scaffolder = Scaffolder("/tmp/shaolin")
  var counter = 1
  val logger = implicitly[spray.util.LoggingContext]
  implicit val timeout = Timeout(2 minutes)

  val apiRoute =
    get {
      pathSingleSlash {
        complete(index)
      } ~
    path("js" / "submit.js") {
      getFromResource("js/submit.js")
        }
    } ~
      post {
        path("submission" / "Palindrome.java") {
          entity(as[String]) { code =>
            val project = scaffolder.makeSbtProject(s"submission-$counter")
            project.addFile("Palindrome.java", code)
            project.addResource("/questions/PalindromeTest.java")
            val bridge = actorRefFactory.actorOf(SbtBridge.props(project.baseDir))
            import SbtBridge._
            val result = bridge ? Test
            complete {
              result collect {
                case SbtSuccess(Test) => "Success!"
                case SbtFailure(Test) => "You failed!"
              }
            }
          }
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