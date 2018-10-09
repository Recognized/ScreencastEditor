package vladsaif.syncedit.plugin.diffview

import com.intellij.util.ui.GraphicsUtil
import vladsaif.syncedit.plugin.ColorSettings
import vladsaif.syncedit.plugin.MergedLineMapping
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Shape
import java.awt.geom.CubicCurve2D
import java.awt.geom.Path2D
import javax.swing.JComponent

class SplitterPainter(
    private val mappingViewModel: MappingViewModel,
    private val leftItemLocator: Locator,
    private val rightItemLocator: Locator
) {

  fun paint(graphics: Graphics, component: JComponent) {
    GraphicsUtil.setupAAPainting(graphics)
    with(graphics as Graphics2D) {
      with(component) {
        color = background
        fillRect(0, 0, width, height)
        val bindings = mappingViewModel.bindings
        val polygons = createShapes(bindings, width)
        for ((top, bottom) in polygons) {
          val path = Path2D.Double()
          path.append(top, true)
          path.append(bottom, true)
          color = ColorSettings.MAPPING_HIGHLIGHT_COLOR
          fill(path)
          color = ColorSettings.MAPPING_BORDER_COLOR
          draw(top)
          draw(bottom)
        }
      }
    }
  }

  // Create list of pair of shapes that enclose area of polygon.
  // Show bindings from transcript to script.
  private fun createShapes(mergedLineMappings: List<MergedLineMapping>, width: Int): List<Pair<Shape, Shape>> {
    val newShapes = mutableListOf<Pair<Shape, Shape>>()
    for ((item, line) in mergedLineMappings) {
      val topLeftCorner = leftItemLocator.locate(item.start).first
      val bottomLeftCorner = leftItemLocator.locate(item.end).second - 1
      val topRightCorner = rightItemLocator.locate(line.start).first
      val bottomRightCorner = rightItemLocator.locate(line.end).second - 1
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