package de.movsim.viewer

import java.awt.Graphics2D
import java.awt.geom._

import org.movsim.roadmappings._
import org.slf4j.LoggerFactory


/**
  * Optimized drawing of RoadSegmentUtils based on the type of their RoadMapping
  */
object PaintRoadMapping {
  private val LOG = LoggerFactory.getLogger(classOf[PaintRoadMapping])
  private val drawBezierPoints = false

  def paintRoadMapping(g: Graphics2D, roadMapping: RoadMapping): Unit = {
    assert(!roadMapping.isPeer, "should not be painted twice")
    val lateralOffset = roadMapping.calcOffsetToCenterline
    LOG.debug("paint roads: roadMapping={}", roadMapping)
    LOG.debug("paint roads: roadWidth={}, laneCount={}", roadMapping.roadWidth, roadMapping.laneCount)
    LOG.debug("paint roads: roadMapping.isPeer={}, lateralOffset={}", roadMapping.isPeer, lateralOffset)
    paintRoadMapping(g, roadMapping, lateralOffset)
  }

  def paintRoadMapping(g: Graphics2D, roadMapping: RoadMapping, lateralOffset: Double): Unit = {
    assert(!roadMapping.isPeer, "should not be painted twice")
    val line = new Line2D.Double
    val from = new Point2D.Double
    val to = new Point2D.Double
    var posTheta : PosTheta = null
    val roadLength = roadMapping.roadLength
    val roadMappingClass = roadMapping.getClass
    if (roadMappingClass eq classOf[RoadMappingLine]) {
      LOG.debug("paint RoadMappingLine={}", roadMapping.toString)
      posTheta = roadMapping.startPos(lateralOffset)
      from.setLocation(posTheta.getScreenX, posTheta.getScreenY)
      posTheta = roadMapping.endPos(lateralOffset)
      to.setLocation(posTheta.getScreenX, posTheta.getScreenY)
      line.setLine(from, to)
      g.draw(line)
      return
    }
    else if (roadMappingClass eq classOf[RoadMappingArc]) {
      val arc = roadMapping.asInstanceOf[RoadMappingArc]
      LOG.debug("lateralOffset={},  paint RoadMappingArc={}", lateralOffset, arc.toString)
      posTheta = roadMapping.startPos
      val angSt = arc.startAngle + (if (arc.clockwise) 0.5 * Math.PI else -0.5 * Math.PI)
      val radius = arc.radius
      val dx = radius * Math.cos(angSt)
      val dy = radius * Math.sin(angSt)
      val arc2D = new Arc2D.Double
      arc2D.setArcByCenter(posTheta.getScreenX - dx, posTheta.getScreenY + dy, radius + lateralOffset * (if (arc.clockwise) 1
      else -1), Math.toDegrees(angSt), Math.toDegrees(arc.arcAngle), Arc2D.OPEN)
      g.draw(arc2D)
      return
    }
    else if (roadMappingClass eq classOf[RoadMappingPoly]) {
      LOG.debug("paint RoadMappingPoly={}", roadMapping.toString)
      val poly = roadMapping.asInstanceOf[RoadMappingPoly]
      import scala.collection.JavaConversions._
      for (map <- poly) {
        paintRoadMapping(g, map, lateralOffset)
      }
      return
    }
    else if (roadMappingClass eq classOf[RoadMappingPolyLine]) {
      LOG.debug("paint RoadMappingPolyLine={}", roadMapping.toString)
      // TODO need to properly handle joins of the lines in the polyline
      if (lateralOffset == 0.0) {
        val polyLine = roadMapping.asInstanceOf[RoadMappingPolyLine]
        val iterator = polyLine.iterator
        if (!iterator.hasNext) return
        val path = new GeneralPath
        var line1 = iterator.next
        posTheta = line1.startPos(lateralOffset)
        path.moveTo(posTheta.getScreenX, posTheta.getScreenY)
        posTheta = line1.endPos(lateralOffset)
        path.lineTo(posTheta.getScreenX, posTheta.getScreenY)
        while ( {
          iterator.hasNext
        }) {
          line1 = iterator.next
          posTheta = line1.endPos(lateralOffset)
          path.lineTo(posTheta.getScreenX, posTheta.getScreenY)
        }
        g.draw(path)
        return
      }
    }
    else if (roadMappingClass eq classOf[RoadMappingBezier]) {
      LOG.debug("paint RoadMappingBezier")
      if (lateralOffset == 0.0) { // TODO remove this zero condition when Bezier lateral offset
        // for control points has been fixed
        // Bezier mapping does not quite give correct control point
        // offsets
        // so only use this if lateral offset is zero (ie not for road
        // edge lines)
        val bezier = roadMapping.asInstanceOf[RoadMappingBezier]
        val path = new GeneralPath
        posTheta = bezier.startPos(lateralOffset)
        path.moveTo(posTheta.getScreenX, posTheta.getScreenY)
        posTheta = bezier.endPos(lateralOffset)
        val cx = bezier.controlX(lateralOffset)
        val cy = bezier.controlY(lateralOffset)
        path.quadTo(cx, cy, posTheta.getScreenX, posTheta.getScreenY)
        g.draw(path)
        return
      }
    }
    else if (roadMappingClass eq classOf[RoadMappingPolyBezier]) {
      LOG.debug("paint RoadMappingPolyBezier")
      if (lateralOffset == 0.0) {
        val polyBezier = roadMapping.asInstanceOf[RoadMappingPolyBezier]
        val iterator = polyBezier.iterator
        if (!iterator.hasNext) return
        val path = new GeneralPath
        var bezier = iterator.next
        posTheta = bezier.startPos(lateralOffset)
        val radius = 10
        val radiusC = 6
        if (drawBezierPoints) g.fillOval(posTheta.getScreenX.toInt - radius / 2, posTheta.getScreenY.toInt - radius / 2, radius, radius)
        path.moveTo(posTheta.getScreenX, posTheta.getScreenY)
        posTheta = bezier.endPos(lateralOffset)
        path.quadTo(bezier.controlX(lateralOffset), bezier.controlY(lateralOffset), posTheta.getScreenX, posTheta.getScreenY)
        if (drawBezierPoints) {
          g.fillOval(posTheta.getScreenX.toInt - radius / 2, posTheta.getScreenY.toInt - radius / 2, radius, radius)
          g.fillOval(bezier.controlX(lateralOffset).toInt - radiusC / 2, bezier.controlY(lateralOffset).toInt - radiusC / 2, radiusC, radiusC)
        }
        while ( {
          iterator.hasNext
        }) {
          bezier = iterator.next
          posTheta = bezier.endPos(lateralOffset)
          path.quadTo(bezier.controlX(lateralOffset), bezier.controlY(lateralOffset), posTheta.getScreenX, posTheta.getScreenY)
          if (drawBezierPoints) {
            g.fillOval(posTheta.getScreenX.toInt - radius / 2, posTheta.getScreenY.toInt - radius / 2, radius, radius)
            g.fillOval(bezier.controlX(lateralOffset).toInt - radiusC / 2, bezier.controlY(lateralOffset).toInt - radiusC / 2, radiusC, radiusC)
          }
        }
        g.draw(path)
        return
      }
    }
    // default drawing
    LOG.debug("draw the road in sections 5 meters long")
    val sectionLength = 5.0
    var roadPos = 0.0
    posTheta = roadMapping.startPos(lateralOffset)
    from.setLocation(posTheta.getScreenX, posTheta.getScreenY)
    while ( {
      roadPos < roadLength
    }) {
      roadPos += sectionLength
      posTheta = roadMapping.map(Math.min(roadPos, roadLength), lateralOffset)
      to.setLocation(posTheta.getScreenX, posTheta.getScreenY)
      line.setLine(from, to)
      g.draw(line)
      from.setLocation(to.getX, to.getY)
    }
  }

  def setClipPath(g: Graphics2D, roadMapping: RoadMapping, clipPath: GeneralPath): Unit = {
    if (roadMapping.clippingPolygons == null) g.setClip(null)
    else {
      clipPath.reset()
      assert(clipPath.getWindingRule == Path2D.WIND_EVEN_ODD)
      // add the clip regions
      import scala.collection.JavaConversions._
      for (polygon <- roadMapping.clippingPolygons) {
        clipPath.moveTo(polygon.getXPoint(0), polygon.getYPoint(0))
        clipPath.lineTo(polygon.getXPoint(1), polygon.getYPoint(1))
        clipPath.lineTo(polygon.getXPoint(2), polygon.getYPoint(2))
        clipPath.lineTo(polygon.getXPoint(3), polygon.getYPoint(3))
        clipPath.lineTo(polygon.getXPoint(0), polygon.getYPoint(0))
      }
      // add the outer region (encloses whole road), so that everything
      // outside the clip
      // region is drawn
      val polygon = roadMapping.outsideClippingPolygon
      clipPath.moveTo(polygon.getXPoint(0), polygon.getYPoint(0))
      clipPath.lineTo(polygon.getXPoint(1), polygon.getYPoint(1))
      clipPath.lineTo(polygon.getXPoint(2), polygon.getYPoint(2))
      clipPath.lineTo(polygon.getXPoint(3), polygon.getYPoint(3))
      clipPath.lineTo(polygon.getXPoint(0), polygon.getYPoint(0))
      clipPath.closePath()
      g.setClip(clipPath)
    }
  }
}

final class PaintRoadMapping private() {
  throw new IllegalStateException("do not instanciate")
}


