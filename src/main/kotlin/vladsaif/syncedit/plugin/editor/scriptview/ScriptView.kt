package vladsaif.syncedit.plugin.editor.scriptview

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBSwingUtilities
import com.intellij.util.ui.UIUtil
import gnu.trove.THashMap
import gnu.trove.TObjectHashingStrategy
import vladsaif.syncedit.plugin.editor.audioview.WaveformGraphics
import vladsaif.syncedit.plugin.editor.audioview.waveform.impl.MouseDragListener
import vladsaif.syncedit.plugin.lang.script.psi.CodeBlock
import vladsaif.syncedit.plugin.model.ScreencastFile
import vladsaif.syncedit.plugin.util.*
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseEvent
import java.util.*
import java.util.concurrent.TimeUnit
import javax.swing.event.ChangeListener

class ScriptView(val screencast: ScreencastFile, givenCoordinator: Coordinator?) : JBPanel<ScriptView>() {
  val coordinator = givenCoordinator ?: LinearCoordinator().apply {
    setTimeUnitsPerPixel(50, TimeUnit.MILLISECONDS)
  }
  private val myBordersRectangles = Cache { calculateBorders(screencast.codeBlockModel.blocks) }
  private val myBlockAreas = Cache { calculateBlockAreas(screencast.codeBlockModel.blocks) }
  private var myTempBorder: DraggedBorder? = null
  private val myDepthDelta: Int = calculateDepthDelta()

  init {
    screencast.codeBlockModel.addChangeListener(ChangeListener {
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
  }

  override fun paint(g: Graphics) {
    with(g as Graphics2D) {
      val metrics = getFontMetrics(ScriptGraphics.FONT)
      font = ScriptGraphics.FONT
      val getLength = { string: String -> metrics.stringWidth(string) }
      for (block in myBlockAreas.get()) {
        drawCodeBlock(block, getLength, metrics)
      }
      myTempBorder?.let { drawTempBorder(it) }
    }
  }

  private fun calculateBorders(blocks: List<CodeBlock>, depth: Int = 0): List<Border> {
    val borders = mutableListOf<Border>()
    for (block in blocks) {
      borders.add(Border(createArea(block.timeRange.start, depth), block, true))
      if (block.isBlock) {
        borders.add(Border(createArea(block.timeRange.end, depth), block, false))
        borders.addAll(calculateBorders(block.innerBlocks, depth + 1))
      }
    }
    return borders
  }

  private fun calculateBlockAreas(
      blocks: List<CodeBlock>,
      colors: Pair<Color, Color> = ScriptGraphics.CODE_BLOCK_BACKGROUND,
      depth: Int = 0
  ): List<BlockArea> {
    val areas = mutableListOf<BlockArea>()
    val (bright, dark) = colors
    val newBright = ScriptGraphics.rotate(bright)
    val newDark = ScriptGraphics.rotate(dark)
    val color = JBColor(bright, dark)
    for (block in blocks) {
      val borders = coordinator.toScreenPixel(block.timeRange.msToNs(), TimeUnit.NANOSECONDS)
      areas.add(BlockArea(borders.start, borders.end, myDepthDelta * depth, block, depth, color))
      areas.addAll(calculateBlockAreas(block.innerBlocks, newBright to newDark, depth + 1))
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
      if (dragStartEvent!!.let { JBSwingUtilities.isLeftMouseButton(it) && !it.isShiftDown && !UIUtil.isControlKeyDown(it) }) {
        val hovered = overBorder(point)
        if (hovered != null) {
          myTempBorder = DraggedBorder(
              point.x,
              screencast.codeBlockModel.findDepth(hovered.codeBlock) * myDepthDelta,
              hovered.codeBlock
          )
          repaint()
        }
      }
    }

    override fun onDrag(point: Point) {
      super.onDrag(point)
      if (myTempBorder != null) {
        myTempBorder!!.x = point.x
        repaint()
      }
    }

    override fun onDragFinished(point: Point) {
      super.onDragFinished(point)
      myTempBorder?.let { }
      myTempBorder = null
      repaint()
    }
  }

  private val myShortenedCode = THashMap<CodeBlock, String>(object : TObjectHashingStrategy<CodeBlock> {
    override fun equals(p0: CodeBlock?, p1: CodeBlock?): Boolean {
      return p0?.timeRange == p1?.timeRange && p0?.code == p1?.code
    }

    override fun computeHashCode(p0: CodeBlock): Int {
      return Objects.hash(p0.timeRange, p0.code)
    }
  })

  private fun Graphics2D.drawTempBorder(border: DraggedBorder) {
    stroke = ScriptGraphics.BORDER_STROKE
    color = WaveformGraphics.WORD_MOVING_SEPARATOR_COLOR
    drawLine(border.x, border.y, border.x, height)
  }

  private fun Graphics2D.drawCodeBlock(
      block: BlockArea,
      getLength: (String) -> Int,
      metrics: FontMetrics
  ) {
    val screenRange = block.x1..block.x2
    val innerRange = screenRange.padded(ScriptGraphics.PADDING.toInt())
    if (block.codeBlock.isBlock) {
      color = block.color
      fillRect(screenRange.start, block.y, screenRange.length, height - block.y)
    }
    stroke = ScriptGraphics.BORDER_STROKE
    color = ScriptGraphics.CODE_BLOCK_BORDER
    drawLine(screenRange.start, block.y, screenRange.start, height)
    if (block.codeBlock.isBlock) {
      drawLine(screenRange.end, block.y, screenRange.end, height)
    }
    val string = myShortenedCode.getOrPut(block.codeBlock) {
      TextFormatter.createEllipsis(block.codeBlock.code, innerRange.length, getLength)
    }
    color = ScriptGraphics.FONT_COLOR
    drawString(string, innerRange.start, block.y + metrics.height + ScriptGraphics.PADDING.toInt())
  }

  data class Border(
      val area: Rectangle,
      val codeBlock: CodeBlock,
      val isLeft: Boolean
  )

  private data class DraggedBorder(
      var x: Int,
      val y: Int,
      val source: CodeBlock
  )

  private data class BlockArea(
      val x1: Int,
      val x2: Int,
      val y: Int,
      val codeBlock: CodeBlock,
      val depth: Int,
      val color: Color
  )

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