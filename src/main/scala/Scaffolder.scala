package shaolin

import java.io.File
import FileUtils._

// inspiration
// https://github.com/sbt/sbt-remote-control/blob/master/integration-tests/src/main/scala/com/typesafe/sbtrc/it/TestUtil.scala

class Scaffolder(scratchDir: File) {
  import Scaffolder._

  def makeSbtProject(relativeDir: String): SbtProject =
    SbtProject(new File(scratchDir, relativeDir))

  def makeSimpleSbtProject(relativeDir: String): SbtProject = {
    val project = SbtProject(new File(scratchDir, relativeDir))
    val mainContent = "object Main extends App { println(\"Hello World\") }\n"
    project.addFile("Hello.scala", mainContent)

    val testContent = """
import org.junit.Assert._
import org.junit._
class FirstPassTest {
    @Test
    def testThatShouldPass: Unit = {
    }
}
class SecondPassTest {
    @Test
    def testThatShouldPass: Unit = {
    }
}
"""
    project.addFile("HelloSpec.scala", testContent)

    project
  }

  /** Creates a dummy project we can run sbt against. */
  def makeFailingSbtProject(relativeDir: String): SbtProject = {
    val project = SbtProject(new File(scratchDir, relativeDir))

    val mainContent = "object Main extends App { println(\"Hello World\") }\n"
    project.addFile("Hello.scala", mainContent)

    val testContent = """
import org.junit.Assert._
import org.junit._
class OnePassOneFailTest {
    @Test
    def testThatShouldPass: Unit = {
    }
    @Test
    def testThatShouldFail: Unit = {
        assertTrue("this is not true", false)
    }
}
class OnePassTest {
    @Test
    def testThatShouldPass: Unit = {
    }
}
class OneFailTest {
    @Test
    def testThatShouldFail: Unit = {
        assertTrue("this is not true", false)
    }
}
"""
    project.addFile("HelloSpec.scala", testContent)

    project
  }

  def cleanAll(): Unit = rmdir(scratchDir)
}

object Scaffolder {
  def apply(path: String): Scaffolder =
    new Scaffolder(ensureDirCreated(new File(path)))
}
