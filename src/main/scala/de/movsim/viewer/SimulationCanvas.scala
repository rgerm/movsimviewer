package de.movsim.viewer

import java.awt.geom.{AffineTransform, GeneralPath, Path2D}
import java.awt.{BasicStroke, Color}
import java.util.Properties

import Viewer.projectMetaData
import org.movsim.autogen.Movsim
import org.movsim.roadmappings.RoadMapping
import org.movsim.simulator.SimulationRunnable.UpdateDrawingCallback
import org.movsim.simulator.Simulator
import org.movsim.simulator.vehicles.Vehicle
import org.movsim.xml.InputLoader

import scala.swing.{Graphics2D, Panel}

class SimulationCanvas(properties: Properties) extends Panel with UpdateDrawingCallback {
  val movsimInput: Movsim = InputLoader.unmarshallMovsim(projectMetaData.getInputFile)
  val simulator = new Simulator(movsimInput)
  val roadNetwork = simulator.getRoadNetwork
  val simulationRunnable = simulator.getSimulationRunnable

  simulator.initialize()
  simulationRunnable.setUpdateDrawingCallback(this)
  simulationRunnable.start()

  import scala.collection.JavaConversions._

  for (roadSegment <- roadNetwork) {
    roadSegment.roadMapping.setRoadColor(Color.gray.getRGB)
  }
  private val brakeLightColor = Color.RED
  var scale = properties.getProperty("initialScale").toDouble
  var xOffset = properties.getProperty("xOffset").toInt
  var yOffset = properties.getProperty("yOffset").toInt
  private val clipPath = new GeneralPath(Path2D.WIND_EVEN_ODD)
  private val vehiclePath = new GeneralPath
  var backgroundChanged = true
  var transform = new AffineTransform
  setTransform()

  def setTransform(): Unit = {
    transform.setToIdentity()
    transform.scale(scale, scale)
    transform.translate(xOffset, yOffset)
  }

  override def paint(g: Graphics2D): Unit = {
    drawBackgroundAndRoadNetwork(g)
    drawMovables(g)
  }

  override def updateDrawing(simulationTime: Double): Unit = {
    repaint()
  }

  def drawRoadSegmentsAndLines(g: Graphics2D): Unit = {
    import scala.collection.JavaConversions._
    for (roadSegment <- roadNetwork) {
      val roadMapping = roadSegment.roadMapping
      if (!roadMapping.isPeer) {
        drawRoadSegment(g, roadMapping)
        //      drawRoadSegmentLines(g, roadMapping)
      }
    }
  }

  def drawRoadSegment(g: Graphics2D, roadMapping: RoadMapping): Unit = {
    val roadStroke: BasicStroke = new BasicStroke(
      roadMapping.roadWidth().toFloat,
      BasicStroke.CAP_BUTT,
      BasicStroke.JOIN_MITER)
    g.setStroke(roadStroke)
    g.setColor(new Color(roadMapping.roadColor()))
    PaintRoadMapping.paintRoadMapping(g, roadMapping)
  }

  def drawBackgroundAndRoadNetwork(g: Graphics2D): Unit = {
    // Draw background here
    g.setColor(Color.green)
    g.fillRect(0, 0, preferredSize.width, preferredSize.height)
    g.setTransform(transform)

    drawRoadSegmentsAndLines(g)

  }

  def vehicleColor(vehicle: Vehicle, simulationTime: Double): Color = {
    Color.blue
  }

  def drawVehicle(g: Graphics2D, simulationTime: Double, roadMapping: RoadMapping, vehicle: Vehicle): Unit = {
    // draw vehicle polygon at new position
    val polygon = roadMapping.mapFloat(vehicle)
    vehiclePath.reset()
    vehiclePath.moveTo(polygon.getXPoint(0), polygon.getYPoint(0))
    vehiclePath.lineTo(polygon.getXPoint(1), polygon.getYPoint(1))
    vehiclePath.lineTo(polygon.getXPoint(2), polygon.getYPoint(2))
    vehiclePath.lineTo(polygon.getXPoint(3), polygon.getYPoint(3))
    vehiclePath.closePath()
    g.setPaint(vehicleColor(vehicle, simulationTime))
    g.fill(vehiclePath)
    if (vehicle.isBrakeLightOn) {
      vehiclePath.reset()
      // points 2 & 3 are at the rear of vehicle
      if (roadMapping.isPeer) {
        vehiclePath.moveTo(polygon.getXPoint(0), polygon.getYPoint(0))
        vehiclePath.lineTo(polygon.getXPoint(1), polygon.getYPoint(1))
      }
      else {
        vehiclePath.moveTo(polygon.getXPoint(2), polygon.getYPoint(2))
        vehiclePath.lineTo(polygon.getXPoint(3), polygon.getYPoint(3))
      }
      vehiclePath.closePath()
      g.setPaint(brakeLightColor)
      g.draw(vehiclePath)
    }
  }

  def drawMovables(g: Graphics2D): Unit = {
    val simulationTime: Double = simulationRunnable.simulationTime
    import scala.collection.JavaConversions._
    for (roadSegment <- roadNetwork) {
      val roadMapping: RoadMapping = roadSegment.roadMapping
      assert(roadMapping != null)
      PaintRoadMapping.setClipPath(g, roadMapping, clipPath)
      import scala.collection.JavaConversions._
      for (vehicle <- roadSegment) {
        drawVehicle(g, simulationTime, roadMapping, vehicle)
      }
//            val vehIter: util.Iterator[Vehicle] = roadSegment.overtakingVehicles
//            while ( {
//              vehIter.hasNext
//            }) {
//              val vehicle: Vehicle = vehIter.next
//              drawVehicle(g, simulationTime, roadMapping, vehicle)
//            }
    }
  }

}
