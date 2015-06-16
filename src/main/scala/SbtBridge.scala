package shaolin

import akka.actor._
import java.io.File
import akka.util.Timeout
import scala.concurrent.duration._
import sbt.client._
import sbt.protocol._
import scala.util.{Failure, Success}

object SbtBridge {
  sealed trait BridgeOp
  case class OpenClient(client: SbtClient) extends BridgeOp
  case object CloseClient extends BridgeOp
  sealed trait Command extends BridgeOp {
    def sbtName: String
  }
  case object Test extends Command {
    def sbtName = "test"
  }

  sealed trait BridgeResult
  case class SbtSuccess(op: Command) extends BridgeResult
  case class SbtFailure(op: Command) extends BridgeResult

  def props(path: File) = Props(new SbtBridge(path))
}

class SbtBridge(location: File) extends Actor with ActorLogging {
  import SbtBridge._
  import context.dispatcher

  // val location = new File("/tmp/java-fun")
  val connector = SbtConnector(configName = "activator",
                               humanReadableName = "Activator",
                               location)
  override val supervisorStrategy = SupervisorStrategy.stoppingStrategy

  @volatile
  var startedConnecting = System.currentTimeMillis()
  var client: Option[SbtClient] = None
  var pending: Option[(Command, ActorRef)] = None

  log.debug("Opening SbtConnector")

  val onConnect = { client: SbtClient =>
    val delta = System.currentTimeMillis() - startedConnecting
    log.debug(s"Connection opened in: ${delta / 1000L} seconds")
    self ! OpenClient(client)
  }
  val onDisconnect = { (reconnecting: Boolean, message: String) =>
    startedConnecting = System.currentTimeMillis()
    log.debug(s"Connection to sbt closed (reconnecting=${reconnecting}: ${message})")
    self ! CloseClient
    if (!reconnecting) {
      log.debug(s"SbtConnector gave up and isn't reconnecting; killing SbtBridge ${self.path.name}")
      self ! PoisonPill
    }
  }
  connector.open(onConnect, onDisconnect)

  def initialise: Receive = {
    case OpenClient(newClient) =>
      log.debug(s"Opening new client actor for sbt client $newClient")
      client = Some(newClient)
      client foreach { _ handleEvents eventListener }
      pending match {
        case None => context.become(idle)
        case Some((cmd, _)) => handleCommand(cmd)
      }
    case Test =>
      pending = Some((Test, context.sender))
  }

  def idle: Receive = {
    case cmd: Command =>
      pending = Some((cmd, context.sender))
      handleCommand(cmd)
    case CloseClient =>
      log.debug(s"Client closed")
      client = None
      context.become(initialise)
  }

  def waitForTests: Receive = {
    case e: ExecutionEngineEvent => handleResult(e)
  }

  def handleResult(e: ExecutionEngineEvent) = {
    (e, pending) match {
      case (_: ExecutionSuccess, Some((op, requester))) =>
        requester ! SbtSuccess(op)
      case (_: ExecutionFailure, Some((op, requester))) =>
        requester ! SbtFailure(op)
    }
    self ! PoisonPill
    // for now SbtBrigde ! Command acts as a transaction and auto-destruct after the answer
    // pending = None
    // context.become(idle)
  }

  def handleCommand(cmd: Command) = {
    client foreach { _.requestExecution(cmd.sbtName, None) }
    context.become(waitForTests)
  }

  def receive = initialise

  val eventListener: EventListener = {
    case e@(ExecutionSuccess(_) | ExecutionFailure(_)) => self ! e
    case _: DetachedLogEvent =>
    case _: TaskStarted =>
    case _: TaskFinished =>
    case e => log.info(e.toString)
  }

  override def postStop() = {
    connector.close()
    // TODO: check if we have something pending and notify? or should
    // we expect the requester to death-watch us?
  }
}