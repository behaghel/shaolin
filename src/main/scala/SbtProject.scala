package shaolin

import java.io.File
import FileUtils._
import scala.concurrent.{Future, ExecutionContext}
import scala.util.Try

case class SbtProject(baseDir: File,
                      sbtVersion: String = SbtProject.defaultSbtTestVersion) {
  import SbtProject._

  val scalaSources = new File(baseDir, "src/main/scala")
  val javaSources = new File(baseDir, "src/main/java")
  val scalaTests = new File(baseDir, "src/test/scala")
  val javaTests = new File(baseDir, "src/test/java")
  val projectDir = new File(baseDir, "project")

  def init()(implicit ec: ExecutionContext): Unit = {
    createDirStructure()
    val buildProps = new File(projectDir, "build.properties")
    createFile(buildProps, "sbt.version=" + sbtVersion)
    val buildContent = s"""
name := "$baseDir"
libraryDependencies += "com.novocode" % "junit-interface" % "0.11" % "test"
"""
    this.addFile("build.sbt", buildContent)
  }

  def createDirStructure(): Unit =
    List(projectDir, scalaSources, javaSources,
      scalaTests, javaTests) foreach ensureDirCreated

  // will simply replace file content if already exists
  def addFile(name: String, content: String)(implicit ec: ExecutionContext): Future[Unit] = {
    val destFile = name match {
      case n if n.endsWith("Spec.scala") => new File(scalaTests, n)
      case n if n.endsWith("Test.java") => new File(javaTests, n)
      case n if n.endsWith(".scala") => new File(scalaSources, n)
      case n if n.endsWith(".java") => new File(javaSources, n)
      case n if n.endsWith(".sbt") => new File(baseDir, n)
      case n => new File(baseDir, n)
    }
    Future { createFile(destFile, content) }
  }

  def addResource(path: String)(implicit ec: ExecutionContext): Future[Unit] = {
    val optInStream = Option(getClass().getResourceAsStream(path))
    val fileName = path.substring(path.lastIndexOf("/")+1)
    optInStream map { inStream =>
      addFile(fileName, scala.io.Source.fromInputStream(inStream).mkString)
    } getOrElse { throw new Exception(s"$path not found in classpath") }
  }
}

object SbtProject {
  // TODO: hook this up to build in some fashion
  val sbt13TestVersion = "0.13.8"
  def defaultSbtTestVersion = sbt13TestVersion

  def bootstrap(baseDir: File,
                sbtVersion: String = SbtProject.defaultSbtTestVersion)
               (implicit ec: ExecutionContext): SbtProject = {
    val project = SbtProject(ensureDirCreated(baseDir), sbtVersion)
    project.init()
    project
  }

  import java.nio.file.Files
  def bootstrapTemporary()(implicit ec: ExecutionContext): SbtProject =
    bootstrap(Files.createTempDirectory("shaolin-").toFile())
}