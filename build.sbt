name := """shaolin"""

version := "1.0"

scalaVersion := "2.11.6"

licenses := Seq("BSD-3" -> url("http://opensource.org/licenses/BSD-3-Clause"))


// Change this to another test framework if you prefer
libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.5" % "test"
// "org.scalacheck" %% "scalacheck" % "1.12.2" % "test"

resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"
val sbtRcVersion = "0.3.5"
val sbtrcClient          = "com.typesafe.sbtrc" % "client-2-11" % sbtRcVersion
// val sbtrcIntegration     = "com.typesafe.sbtrc" % "integration-tests" % sbtRcVersion

// Uncomment to use Akka
val akkaVersion = "2.3.11"
val akka = "com.typesafe.akka" %% "akka-actor" % akkaVersion
val akkaTesting = "com.typesafe.akka" %% "akka-testkit" % akkaVersion % "test"
val akkaLogging = "com.typesafe.akka" %% "akka-slf4j" % akkaVersion

val slf4j = "org.slf4j" % "slf4j-api" % "1.7.12"
val logback = "ch.qos.logback" % "logback-classic" % "1.1.3"

val sprayVersion = "1.3.3"
val sprayCan = "io.spray" %% "spray-can" % sprayVersion
val sprayRouting = "io.spray" %% "spray-routing" % sprayVersion
val sprayTestkit = "io.spray" %% "spray-testkit" % sprayVersion % "test"
val sprayJson = "io.spray" %%  "spray-json" % "1.3.2"
// "io.spray" %%  "spray-client" % sprayVersion
Revolver.settings

libraryDependencies ++= Seq(//sbtrcClient, << UNCOMMENT once available from Heroku
                            // https://github.com/typesafehub/activator/issues/979
                            // https://github.com/sbt/sbt/issues/2054
                            //sbtrcIntegration,
                            akka, akkaTesting, akkaLogging, slf4j, logback,
                            sprayCan, sprayRouting, sprayTestkit, sprayJson)


enablePlugins(JavaAppPackaging)

libraryDependencies += "com.lihaoyi" % "ammonite-repl" % "0.3.2" % "test" cross CrossVersion.full

initialCommands in (Test, console) := """ammonite.repl.Repl.run("")"""

libraryDependencies += "com.lihaoyi" %% "ammonite-ops" % "0.3.2" % "test"

val formerInitialCommands = """
import akka.actor._
import akka.pattern.ask
import shaolin.SbtBridge
import SbtBridge._
import akka.util.Timeout
import scala.concurrent.duration._
val sys = ActorSystem("shaolin")
val sb = sys.actorOf(props(new java.io.File("/tmp/java-fun")))
implicit val timeout = Timeout(2.seconds)
"""
