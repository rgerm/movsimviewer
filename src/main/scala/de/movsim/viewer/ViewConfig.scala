package de.movsim.viewer

import java.io._
import java.util.Properties

import org.movsim.input.ProjectMetaData
import org.slf4j.{Logger, LoggerFactory}

object ViewConfig {

  val logger: Logger = LoggerFactory.getLogger(classOf[ViewConfig])

  val defaultPropertyName: String = "/config/defaultviewerconfig.properties"

  private var defaultProperties: Properties = _

  def loadDefaultProperties(): Properties = {
    if (defaultProperties == null) {
      defaultProperties = new Properties()
      try {
        logger.info("read default properties from file " + defaultPropertyName)
        val is: InputStream = classOf[ViewConfig].getResourceAsStream(defaultPropertyName)
        defaultProperties.load(is)
        is.close()
        defaultProperties = new Properties(defaultProperties)
      } catch {
        case e: FileNotFoundException => {e.printStackTrace()}
        case e: IOException => e.printStackTrace()

      }
    }
    defaultProperties
  }

  def loadProperties(projectMetaData: ProjectMetaData): Properties = {
    if (projectMetaData.hasProjectName()) {
      loadProperties(projectMetaData.getProjectName, projectMetaData.getPathToProjectFile)
    }
    loadDefaultProperties()
  }

  def loadProperties(projectName: String, path: String): Properties = {
    val applicationProps: Properties = loadDefaultProperties()
    val file: File = new File(path + projectName + ".properties")
    try {
      logger.debug(
        "try to read from file=" + file.getName + ", path=" +
          file.getAbsolutePath)
      if (ProjectMetaData.getInstance.isXmlFromResources) {
        val inputStream: InputStream =
          classOf[ViewConfig].getResourceAsStream(file.toString)
        if (inputStream != null) {
          applicationProps.load(inputStream)
          inputStream.close()
        }
      } else {
        val in: InputStream = new FileInputStream(file)
        applicationProps.load(in)
        in.close()
      }
    } catch {
      case e: FileNotFoundException =>
        logger.info("cannot find " + file.toString + ". Fall back to default properties.")
      case e: IOException => e.printStackTrace()

    }
    applicationProps
  }

}

final class ViewConfig private() {
  throw new IllegalStateException("do not instanciate")
}
