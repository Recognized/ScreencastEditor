package vladsaif.syncedit.plugin.diffview

import com.intellij.util.ui.GraphicsUtil
import vladsaif.syncedit.plugin.IRange
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Shape
import java.awt.geom.CubicCurve2D
import java.awt.geom.Path2D
import javax.swing.JComponent

class SplitterPainter(
    private val bindProvider: BindProvider,
    private val leftItemLocator: Locator,
    private val rightItemLocator: Locator
) {
  private val fillerColor: Color get() = Color(0, 200, 0, 30)
  private val borderColor: Color get() = Color(0, 200, 0, 70)

  fun paint(graphics: Graphics, component: JComponent) {
    GraphicsUtil.setupAAPainting(graphics)
    with(graphics as Graphics2D) {
      with(component) {
        color = background
        fillRect(0, 0, width, height)
        val bindings = bindProvider.getBindings()
        val polygons = createShapes(bindings, width)
        val path = Path2D.Double()
        for ((top, bottom) in polygons) {
          path.append(top, true)
          path.append(bottom, true)
          color = fillerColor
          fill(path)
          color = borderColor
          draw(top)
          draw(bottom)
        }
      }
    }
  }

  // Create list of pair of shapes that enclose area of polygon.
  // Show bindings from transcript to script.
  private fun createShapes(bindings: Map<IRange, IRange>, width: Int): List<Pair<Shape, Shape>> {
    val newShapes = mutableListOf<Pair<Shape, Shape>>()
    for ((key, value) in bindings) {
      val topLeftCorner = leftItemLocator.locate(key.start).first
      val bottomLeftCorner = leftItemLocator.locate(key.end).second
      val topRightCorner = rightItemLocator.locate(value.start).first
      val bottomRightCorner = rightItemLocator.locate(value.end).second
      val topShape = createCubicCurve(topLeftCorner, topRightCorner, width, true)
      // Should be reversed order, because then we constructing path
      val bottomShape = createCubicCurve(bottomLeftCorner, bottomRightCorner, width, false)
      newShapes.add(topShape to bottomShape)
    }
    return newShapes
  }

  companion object {
    private const val CTRL_PROXIMITY_X = 0.3

    private fun createCubicCurve(y1: Int, y2: Int, width: Int, forward: Boolean): CubicCurve2D.Double {
      return if (forward) {
        CubicCurve2D.Double(0.0, y1.toDouble(),
            width * CTRL_PROXIMITY_X, y1.toDouble(),
            width * (1.0 - CTRL_PROXIMITY_X), y2.toDouble(),
            width.toDouble(), y2.toDouble())
      } else {
        CubicCurve2D.Double(width.toDouble(), y2.toDouble(),
            width * (1.0 - CTRL_PROXIMITY_X), y2.toDouble(),
            width * CTRL_PROXIMITY_X, y1.toDouble(),
            0.0, y1.toDouble())
      }
    }
  }
}