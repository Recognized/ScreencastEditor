package vladsaif.syncedit.plugin.editor.scriptview

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBSwingUtilities
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import gnu.trove.THashMap
import gnu.trove.TObjectIdentityHashingStrategy
import vladsaif.syncedit.plugin.editor.audioview.WaveformGraphics
import vladsaif.syncedit.plugin.editor.audioview.waveform.impl.MouseDragListener
import vladsaif.syncedit.plugin.lang.script.psi.Block
import vladsaif.syncedit.plugin.lang.script.psi.Code
import vladsaif.syncedit.plugin.lang.script.psi.Statement
import vladsaif.syncedit.plugin.model.ScreencastFile
import vladsaif.syncedit.plugin.util.*
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseEvent
import java.util.concurrent.TimeUnit
import javax.swing.event.ChangeListener
import kotlin.math.max
import kotlin.math.min

class ScriptView(val screencast: ScreencastFile, givenCoordinator: Coordinator?) : JBPanel<ScriptView>() {
  val coordinator = givenCoordinator ?: LinearCoordinator().apply {
    setTimeUnitsPerPixel(50, TimeUnit.MILLISECONDS)
  }
  private val myBordersRectangles =
    Cache { calculateBorders(screencast.codeModel.blocks).sortedByDescending { it.area.y } }
  private val myBlockAreas = Cache { calculateCodeAreas(screencast.codeModel.blocks) }
  private var myTempBorder: DraggedBorder? = null
  private val myShortenedCode = THashMap<Code, String>(TObjectIdentityHashingStrategy())
  private val myDepthDelta: Int = calculateDepthDelta()
  private val myFurtherMostBorder = Cache { findPreferredWidth() }

  val preferredWidth get() = myFurtherMostBorder.get()

  private fun findPreferredWidth(): Int {
    return coordinator.toScreenPixel(
      screencast.codeModel.blocks.lastOrNull()?.endTime?.toLong() ?: 0L,
      TimeUnit.MILLISECONDS
    )
  }

  init {
    screencast.codeModel.addChangeListener(ChangeListener {
      resetCache()
      revalidate()
      repaint()
    })
    addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent?) {
        myBordersRectangles.resetCache()
      }
    })
  }

  override fun getPreferredSize(): Dimension {
    return Dimension(
      preferredWidth + JBUI.scale(200),
      (myBordersRectangles.get().lastOrNull()?.area?.y ?: 0) + myDepthDelta
    )
  }

  fun installListeners() {
    addMouseListener(myDragBorderListener)
    addMouseMotionListener(myDragBorderListener)
  }

  fun overBorder(point: Point): Border? {
    // TODO: optimize
    for (border in myBordersRectangles.get()) {
      if (point in border.area) {
        return border
      }
    }
    return null
  }

  fun resetCache() {
    myBordersRectangles.resetCache()
    myBlockAreas.resetCache()
    myShortenedCode.clear()
    myFurtherMostBorder.resetCache()
  }

  override fun paint(g: Graphics) {
    with(g as Graphics2D) {
      val metrics = getFontMetrics(ScriptGraphics.FONT)
      font = ScriptGraphics.FONT
      val getLength = { string: String -> metrics.stringWidth(string) }
      for (block in myBlockAreas.get()) {
        drawCode(block, getLength, metrics)
      }
      color = ScriptGraphics.CODE_BLOCK_BORDER
      stroke = BasicStroke(ScriptGraphics.BORDER_WIDTH)
      for (block in myBlockAreas.get()) {
        drawBorderMark(block is CodeArea.BlockArea, block.x1, block.x2, block.y)
      }
      myTempBorder?.let { drawTempBorder(it) }
    }
  }

  private fun Graphics2D.drawBorderMark(isBlock: Boolean, x1: Int, x2: Int, y: Int) {
    if (isBlock) {
      drawLine(x1, y, min(x1 + ScriptGraphics.PADDING.toInt(), x2), y)
      drawLine(max(x1, x2 - ScriptGraphics.PADDING.toInt()), y, x2, y)
    } else {
      drawLine(
        x1 - (ScriptGraphics.PADDING / 2).toInt(), y,
        x1 + (ScriptGraphics.PADDING / 2).toInt(), y
      )
    }
  }

  private fun calculateBorders(codeList: List<Code>, depth: Int = 0): List<Border> {
    val borders = mutableListOf<Border>()
    for (code in codeList) {
      borders.add(Border(createArea(code.startTime, depth), code, true))
      if (code is Block) {
        borders.add(Border(createArea(code.endTime, depth), code, false))
        borders.addAll(calculateBorders(code.innerBlocks, depth + 1))
      }
    }
    return borders
  }

  private fun calculateCodeAreas(
    codeList: List<Code>,
    colors: Pair<Color, Color> = ScriptGraphics.CODE_BLOCK_BACKGROUND,
    parent: Block? = null,
    depth: Int = 0
  ): List<CodeArea> {
    val areas = mutableListOf<CodeArea>()
    val (bright, dark) = colors
    val newBright = ScriptGraphics.rotate(bright)
    val newDark = ScriptGraphics.rotate(dark)
    val color = JBColor(bright, dark)
    for ((index, code) in codeList.withIndex()) {
      val y = myDepthDelta * depth
      when (code) {
        is Block -> {
          val borders = coordinator.toScreenPixel(code.timeRange.msToNs(), TimeUnit.NANOSECONDS)
          areas.add(CodeArea.BlockArea(borders.start, borders.end, y, code, color))
          areas.addAll(calculateCodeAreas(code.innerBlocks, newBright to newDark, code, depth + 1))
        }
        is Statement -> {
          val borderLeft = coordinator.toScreenPixel(code.timeOffset.toLong(), TimeUnit.MILLISECONDS)
          val borderRight = if (index + 1 < codeList.size) {
            coordinator.toScreenPixel(codeList[index + 1].startTime.toLong(), TimeUnit.MILLISECONDS)
          } else {
            coordinator.toScreenPixel(
              parent?.timeRange?.end?.toLong() ?: coordinator.toNanoseconds(width) / 1_000_000,
              TimeUnit.MILLISECONDS
            )
          }
          areas.add(CodeArea.StatementArea(borderLeft, borderRight, y, code))
        }
      }
    }
    return areas
  }

  private fun createArea(timeOffset: Int, depth: Int): Rectangle {
    val x = coordinator.toScreenPixel(timeOffset.toLong(), TimeUnit.MILLISECONDS)
    return Rectangle(
      x - ScriptGraphics.BORDER_PRECISION,
      depth * myDepthDelta,
      ScriptGraphics.BORDER_PRECISION * 2,
      height - depth * myDepthDelta
    )
  }

  private fun calculateDepthDelta(): Int {
    val metrics = getFontMetrics(ScriptGraphics.FONT)
    return metrics.height * 2 + ScriptGraphics.PADDING.toInt()
  }

  private val myDragBorderListener = object : MouseDragListener() {

    override fun mouseMoved(e: MouseEvent?) {
      e ?: return
      super.mouseMoved(e)
      if (!e.isShiftDown && !UIUtil.isControlKeyDown(e) && overBorder(e.point) != null) {
        e.component?.cursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)
      } else {
        e.component?.cursor = Cursor.getDefaultCursor()
      }
    }

    override fun onDragStarted(point: Point) {
      super.onDragStarted(point)
      if (dragStartEvent!!.let {
          JBSwingUtilities.isLeftMouseButton(it) && !it.isShiftDown && !UIUtil.isControlKeyDown(
            it
          )
        }) {
        val hovered = overBorder(point)
        if (hovered != null) {
          val (left, right) = when (hovered.codeBlock) {
            is Block -> screencast.codeModel.findDragBoundary(hovered.codeBlock, hovered.isLeft)
            is Statement -> screencast.codeModel.findDragBoundary(hovered.codeBlock)
          }
          val trueLeft = if (left == -1) 0 else left
          val trueRight = if (right == -1) (coordinator.toNanoseconds(width) / 1_000_000).toInt() else right
          val allowedRange = coordinator.toScreenPixel((trueLeft..trueRight).msToNs(), TimeUnit.NANOSECONDS)
          myTempBorder = DraggedBorder(
            point.x,
            screencast.codeModel.findDepth(hovered.codeBlock) * myDepthDelta,
            hovered.isLeft,
            allowedRange,
            hovered.codeBlock
          )
          repaint()
        }
      }
    }

    override fun onDrag(point: Point) {
      super.onDrag(point)
      if (myTempBorder != null) {
        myTempBorder!!.x = point.x.coerceIn(myTempBorder!!.allowedRange)
        repaint()
      }
    }

    override fun onDragFinished(point: Point) {
      super.onDragFinished(point)
      myTempBorder?.let { updateBlock(it) }
      myTempBorder = null
      repaint()
    }
  }

  private fun updateBlock(newBorder: DraggedBorder) {
    val newPoint = (coordinator.toNanoseconds(newBorder.x) / 1_000_000).toInt()
    val newCode = when (newBorder.source) {
      is Statement -> Statement(newBorder.source.code, newPoint)
      is Block -> {
        Block(
          newBorder.source.code,
          newBorder.source.timeRange.let { if (newBorder.isLeft) newPoint..it.end else it.start..newPoint },
          newBorder.source.innerBlocks
        )
      }
    }
    screencast.codeModel.replace(newBorder.source, newCode)
  }

  private fun Graphics2D.drawTempBorder(border: DraggedBorder) {
    stroke = ScriptGraphics.BORDER_STROKE
    color = WaveformGraphics.WORD_MOVING_SEPARATOR_COLOR
    drawLine(border.x, border.y, border.x, height)
    when {
      border.source is Statement -> drawBorderMark(false, border.x, 0, border.y)
      border.isLeft -> drawLine(border.x, border.y, border.x + ScriptGraphics.PADDING.toInt(), border.y)
      else -> drawLine(border.x - ScriptGraphics.PADDING.toInt(), border.y, border.x, border.y)
    }
  }

  private fun Graphics2D.drawCode(
    block: CodeArea,
    getLength: (String) -> Int,
    metrics: FontMetrics
  ) {
    val screenRange = block.x1..block.x2
    val innerRange = screenRange.padded(ScriptGraphics.PADDING.toInt())
    if (block is CodeArea.BlockArea) {
      color = block.color
      fillRect(screenRange.start, block.y, screenRange.length, height - block.y)
    }
    stroke = ScriptGraphics.BORDER_STROKE
    color = ScriptGraphics.CODE_BLOCK_BORDER
    drawLine(screenRange.start, block.y, screenRange.start, height)
    if (block is CodeArea.BlockArea) {
      drawLine(screenRange.end, block.y, screenRange.end, height)
    }
    val string = myShortenedCode.getOrPut(block.code) {
      TextFormatter.createEllipsis(block.code.code, innerRange.length, getLength)
    }
    color = ScriptGraphics.FONT_COLOR
    drawString(string, innerRange.start, block.y + metrics.height + ScriptGraphics.PADDING.toInt())
  }

  data class Border(
    val area: Rectangle,
    val codeBlock: Code,
    val isLeft: Boolean
  )

  private data class DraggedBorder(
    var x: Int,
    val y: Int,
    val isLeft: Boolean,
    val allowedRange: IntRange,
    val source: Code
  )

  private sealed class CodeArea(val x1: Int, val x2: Int, val y: Int, val code: Code) {

    class BlockArea(
      x1: Int,
      x2: Int,
      y: Int,
      block: Block,
      val color: Color
    ) : CodeArea(x1, x2, y, block)

    class StatementArea(
      x1: Int,
      x2: Int,
      y: Int,
      statement: Statement
    ) : CodeArea(x1, x2, y, statement)
  }

  private class Cache<T>(private val recalculate: () -> T) {
    private var myStorage: T? = null

    fun get(): T {
      return if (myStorage != null) myStorage!! else recalculate().also { myStorage = it }
    }

    fun resetCache() {
      myStorage = null
    }
  }
}