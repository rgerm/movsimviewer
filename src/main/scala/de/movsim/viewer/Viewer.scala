package de.movsim.viewer

import java.util.Properties

import org.movsim.input.{MovsimCommandLine, ProjectMetaData}

import scala.swing._
import scala.swing.event.{MouseClicked, MouseDragged, MousePressed}


object Viewer extends SimpleSwingApplication {
  var properties : Properties = new Properties()
  val projectMetaData: ProjectMetaData = ProjectMetaData.getInstance

  override def main(args: Array[String]): Unit = {
    MovsimCommandLine.parse(args)
    properties = ViewConfig.loadProperties(projectMetaData)
    super.main(args)
  }

  def top: Frame = new MainFrame {
    var xOffsetSave : Int = 0
    var yOffsetSave : Int = 0
    var startDragX : Int = 0
    var startDragY : Int = 0

    title = projectMetaData.getProjectName

    val canvas = new SimulationCanvas(properties) {
      preferredSize = new Dimension(1000, 800)
    }

    contents = new BorderPanel {
//      layout(gridPanel) = North
//      layout(button) = West
      layout(canvas) = BorderPanel.Position.Center
//      layout(toggle) = East
//      layout(textField) = South
    }

    listenTo(canvas.mouse.clicks, canvas.mouse.moves)

    reactions += {
      case MouseClicked(_, point, _, _, _) => println("  -- mouse clicked x=" + point.x + "    y=" + point.y)
      case e: MousePressed => startDrag(e)
      case e: MouseDragged => doDrag(e)
    }

    def startDrag(e: MousePressed): Unit = {
      val p = e.point
      startDragX = p.x
      startDragY = p.y
      xOffsetSave = canvas.xOffset
      yOffsetSave = canvas.yOffset
    }

    def doDrag(e: MouseDragged): Unit = {
      val p = e.point
      val xOffsetNew : Int = xOffsetSave + ((p.x - startDragX) / canvas.scale).toInt
      val yOffsetNew : Int = yOffsetSave + ((p.y - startDragY) / canvas.scale).toInt
      if ((xOffsetNew != canvas.xOffset) || (yOffsetNew != canvas.yOffset)) {
        canvas.xOffset = xOffsetNew
        canvas.yOffset = yOffsetNew
        canvas.setTransform()
      }
    }
  }
}
