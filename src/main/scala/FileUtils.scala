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

}