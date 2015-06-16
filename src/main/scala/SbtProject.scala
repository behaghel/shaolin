package shaolin

import java.io.File
import FileUtils._

case class SbtProject(baseDir: File,
                      sbtVersion: String = SbtProject.defaultSbtTestVersion) {
  import SbtProject._

  val scalaSources = new File(baseDir, "src/main/scala")
  val javaSources = new File(baseDir, "src/main/java")
  val scalaTests = new File(baseDir, "src/test/scala")
  val javaTests = new File(baseDir, "src/test/java")
  val projectDir = new File(baseDir, "project")

  init()

  def init(): Unit = {
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

  def addFile(name: String, content: String): Unit = {
    val destFile = name match {
      case n if n.endsWith("Spec.scala") => new File(scalaTests, n)
      case n if n.endsWith("Test.java") => new File(javaTests, n)
      case n if n.endsWith(".scala") => new File(scalaSources, n)
      case n if n.endsWith(".java") => new File(javaSources, n)
      case n if n.endsWith(".sbt") => new File(baseDir, n)
      case n => new File(baseDir, n)
    }
    createFile(destFile, content)
  }

  def addResource(path: String): Unit = {
    val inStream = getClass().getResourceAsStream(path)
    val fileName = path.substring(path.lastIndexOf("/")+1)
    addFile(fileName, scala.io.Source.fromInputStream(inStream).mkString)
  }
}

object SbtProject {
  // TODO: hook this up to build in some fashion
  val sbt13TestVersion = "0.13.8"
  def defaultSbtTestVersion = sbt13TestVersion
}