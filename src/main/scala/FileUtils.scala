package shaolin

import java.io.File

object FileUtils {

  def createFile(name: File, content: String): Unit = {
    val writer = new java.io.FileWriter(name)
    try writer.write(content)
    finally writer.close()
  }

  def rmdir(dir: File): Unit = {
    for (sub <- dir.listFiles()) {
      if (sub.isDirectory())
        rmdir(sub)
      else sub.delete()
    }
    dir.delete()
  }

  def ensureDirCreated(dir: File): File = {
    if (!dir.isDirectory()) dir.mkdirs()
    dir
  }

  def base(path: String): (String, String) =
      path.splitAt(path.lastIndexOf("/"))

  val javaSourceFileR = "\\.java$".r
  val javaTestFileSuffix = "Test.java"
  val scalaSourceFileR = "\\.scala$".r
  val scalaTestFileSuffix = "Spec.scala"
  /** Return the canonical name of the test for the given file.
    * `filename` must be the basename and not a whole path. */
  def source2test(filename: String): String =
    scalaSourceFileR.replaceFirstIn(
      javaSourceFileR.replaceFirstIn(filename, javaTestFileSuffix),
      scalaTestFileSuffix)
}