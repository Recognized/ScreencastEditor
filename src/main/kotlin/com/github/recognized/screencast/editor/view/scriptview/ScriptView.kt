package com.github.recognized.screencast.editor.view.scriptview

import com.github.recognized.kotlin.ranges.extensions.length
import com.github.recognized.screencast.editor.lang.script.psi.Block
import com.github.recognized.screencast.editor.lang.script.psi.Code
import com.github.recognized.screencast.editor.lang.script.psi.Statement
import com.github.recognized.screencast.editor.model.Screencast
import com.github.recognized.screencast.editor.util.*
import com.github.recognized.screencast.editor.util.Cache.Companion.cache
import com.github.recognized.screencast.editor.view.audioview.WaveformGraphics
import com.github.recognized.screencast.editor.view.audioview.waveform.DraggableXAxis
import com.github.recognized.screencast.editor.view.audioview.waveform.impl.DragXAxisListener
import com.github.recognized.screencast.editor.view.audioview.waveform.impl.MouseDragListener
import com.intellij.openapi.Disposable
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBSwingUtilities
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import gnu.trove.THashMap
import gnu.trove.TObjectIdentityHashingStrategy
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseEvent
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ScriptView(
  val screencast: Screencast
) :
  JBPanel<ScriptView>(),
  DraggableXAxis,
  DrawingFixture by DrawingFixture.create(),
  Disposable {

  inline val coordinator get() = screencast.coordinator
  private val myBordersRectangles = cache { calculateBorders(screencast.codeModel.codes).sortedWith(AREA_COMPARATOR) }
  private val myBlockAreas = cache { calculateCodeAreas(screencast.codeModel.codes) }
  private var myTempBorder: DraggedBorder? = null
  private val myShortenedCode = THashMap<Code, String>(TObjectIdentityHashingStrategy())
  private val myDepthDelta: Int = calculateDepthDelta()
  private val myFurtherMostBorder = cache { lastBlockPixel() }
  private val myFirstBorder = cache { firstBlockPixel() }
  private val myCodeListener = {
    resetCache()
    revalidate()
    repaint()
  }
  private val myDragWholeScriptListener = object : DragXAxisListener() {
    override fun onDragAction() {
      repaint()
    }

    override fun onDragFinishedAction(delta: Int) {
      fixScriptDelta(delta)
      resetCache()
    }
  }

  init {
    screencast.addCodeListener(myCodeListener)
    addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent?) {
        myBordersRectangles.resetCache()
      }
    })
  }

  override fun dispose() {
    screencast.removeCodeListener(myCodeListener)
  }

  override fun getPreferredSize(): Dimension {
    return Dimension(
      myFurtherMostBorder.get().divScale() + 200,
      (myBordersRectangles.get().lastOrNull()?.area?.y ?: 0) + myDepthDelta
    )
  }

  fun installListeners() {
    myDragBorderListener.install(this)
  }

  override fun activateXAxisDrag() {
    myDragBorderListener.uninstall(this)
    myDragWholeScriptListener.install(this)
  }

  override fun deactivateXAxisDrag() {
    myDragBorderListener.install(this)
    myDragWholeScriptListener.uninstall(this)
  }

  fun overBorder(point: Point): Border? {
    val scaled = Point(point.x.mulScale(), point.y)
    for (border in myBordersRectangles.get()) {
      if (scaled in border.area) {
        return border
      }
    }
    return null
  }

  fun resetCache() {
    Cache.resetCache(this)
    myShortenedCode.clear()
  }

  override fun paint(g: Graphics) {
    with(g as Graphics2D) {
      setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
      val dragDelta = myDragWholeScriptListener.delta
      if (dragDelta != 0) {
        val delta = max(dragDelta.toDouble(), -myFirstBorder.get() / JBUI.pixScale().toDouble())
        translate(delta, 0.0)
      }
      val metrics = getFontMetrics(ScriptGraphics.FONT)
      font = ScriptGraphics.FONT
      val getLength = { string: String -> metrics.stringWidth(string) }
      for (block in myBlockAreas.get()) {
        drawCode(block, getLength, metrics)
      }
      color = ScriptGraphics.CODE_BLOCK_BORDER
      stroke = BasicStroke(ScriptGraphics.BORDER_WIDTH)
      for (block in myBlockAreas.get()) {
        drawBorderMark(block is CodeArea.BlockArea, block.x1, block.x2, block.y.toFloat())
      }
      myTempBorder?.let { drawTempBorder(it) }
    }
  }

  private fun Graphics2D.drawBorderMark(isBlock: Boolean, x1: Int, x2: Int, y: Float) {
    val x2scaled = x2.divScaleF()
    val x1scaled = x1.divScaleF()
    if (isBlock) {
      drawLine(
        x1scaled, y,
        min(x1scaled + ScriptGraphics.PADDING, x2scaled), y
      )
      drawLine(
        max(x1scaled, x2scaled - ScriptGraphics.PADDING.toInt()), y,
        x2scaled, y
      )
    } else {
      drawLine(
        x1scaled - (ScriptGraphics.PADDING / 2).toInt(), y,
        x1scaled + (ScriptGraphics.PADDING / 2).toInt(), y
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


  private fun lastBlockPixel(): Int {
    return coordinator.toPixel(
      screencast.codeModel.codes.lastOrNull()?.endTime?.toLong() ?: 0L,
      TimeUnit.MILLISECONDS
    )
  }

  private fun firstBlockPixel(): Int {
    return coordinator.toPixel(
      screencast.codeModel.codes.firstOrNull()?.startTime?.toLong() ?: 0L,
      TimeUnit.MILLISECONDS
    )
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
          val borders = coordinator.toPixelRange(code.timeRange, TimeUnit.MILLISECONDS)
          areas.add(CodeArea.BlockArea(borders.start, borders.endInclusive, y, code, color))
          areas.addAll(calculateCodeAreas(code.innerBlocks, newBright to newDark, code, depth + 1))
        }
        is Statement -> {
          val borderLeft = coordinator.toPixel(code.timeOffset.toLong(), TimeUnit.MILLISECONDS)
          val borderRight = if (index + 1 < codeList.size) {
            coordinator.toPixel(codeList[index + 1].startTime.toLong(), TimeUnit.MILLISECONDS)
          } else {
            coordinator.toPixel(
              parent?.timeRange?.endInclusive?.toLong() ?: coordinator.toNanoseconds(width) / 1_000_000,
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
    val x = coordinator.toPixel(timeOffset.toLong(), TimeUnit.MILLISECONDS)
    return Rectangle(
      x - ScriptGraphics.BORDER_PRECISION.mulScale(),
      depth * myDepthDelta,
      ScriptGraphics.BORDER_PRECISION.mulScale() * 2,
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
              ?: let { resetCache(); return }
            is Statement -> screencast.codeModel.findDragBoundary(hovered.codeBlock)
              ?: let { resetCache(); return }
          }
          val trueLeft = if (left == -1) 0 else left
          val trueRight = if (right == -1) (coordinator.toNanoseconds(width.mulScale()) / 1_000_000).toInt() else right
          val allowedRange =
            coordinator.toPixelRange(trueLeft..trueRight, TimeUnit.MILLISECONDS)
          myTempBorder = DraggedBorder(
            point.x.mulScale(),
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
        myTempBorder!!.x = point.x.mulScale().coerceIn(myTempBorder!!.allowedRange)
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

  private fun fixScriptDelta(dragDelta: Int) {
    if (dragDelta == 0) return
    val delta = max(dragDelta, -myFirstBorder.get().divScale())
    val start = coordinator.toNanoseconds(myFirstBorder.get())
    val end = coordinator.toNanoseconds(myFirstBorder.get() + delta.mulScale())
    screencast.performModification {
      codeModel.shiftAll(end - start, TimeUnit.NANOSECONDS)
    }
  }

  private fun updateBlock(newBorder: DraggedBorder) {
    val newPoint = (coordinator.toNanoseconds(newBorder.x) / 1_000_000).toInt()
    val newCode = when (newBorder.source) {
      is Statement -> Statement(newBorder.source.code, newPoint)
      is Block -> {
        Block(
          newBorder.source.code,
          newBorder.source.timeRange.let { if (newBorder.isLeft) newPoint..it.endInclusive else it.start..newPoint },
          newBorder.source.innerBlocks
        )
      }
    }
    screencast.performModification {
      codeModel.replace(newBorder.source, newCode)
    }
  }

  private fun Graphics2D.drawTempBorder(border: DraggedBorder) {
    stroke = ScriptGraphics.BORDER_STROKE
    color = WaveformGraphics.WORD_MOVING_SEPARATOR_COLOR
    val x = border.x.divScaleF()
    val y = border.y.toFloat()
    drawLine(x, y, x, height.toFloat())
    when {
      border.source is Statement -> drawBorderMark(false, border.x, 0, y)
      border.isLeft -> drawLine(x, y, x + ScriptGraphics.PADDING.toInt(), y)
      else -> drawLine(x - ScriptGraphics.PADDING.toInt(), y, x, y)
    }
  }

  private fun Graphics2D.drawCode(
    block: CodeArea,
    getLength: (String) -> Int,
    metrics: FontMetrics
  ) {
    val screenRange = block.x1..block.x2
    if (block is CodeArea.BlockArea) {
      color = block.color
      fillRect(
        screenRange.start.divScaleF(),
        block.y.toFloat(),
        screenRange.length.divScaleF(),
        (height - block.y).toFloat()
      )
    }
    stroke = ScriptGraphics.BORDER_STROKE
    color = ScriptGraphics.CODE_BLOCK_BORDER
    drawLine(screenRange.start.divScaleF(), block.y.toFloat(), screenRange.start.divScaleF(), height.toFloat())
    if (block is CodeArea.BlockArea) {
      drawLine(
        screenRange.endInclusive.divScaleF(),
        block.y.toFloat(),
        screenRange.endInclusive.divScaleF(),
        height.toFloat()
      )
    }
    val inStart = screenRange.start.divScaleF() + ScriptGraphics.PADDING
    val inEnd = screenRange.endInclusive.divScaleF() - ScriptGraphics.PADDING
    val length = max((inEnd - inStart).roundToInt(), 0)
    val string = myShortenedCode.getOrPut(block.code) {
      TextFormatter.createEllipsis(block.code.code, length, getLength)
    }
    color = ScriptGraphics.FONT_COLOR
    if (!string.isEmpty()) {
      drawString(string, inStart, block.y + metrics.height + ScriptGraphics.PADDING)
    }
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


  companion object {
    private val AREA_COMPARATOR = Comparator
      .comparingInt<ScriptView.Border> { it.area.y }
      .reversed()
      .thenComparing(Comparator.comparingInt { it.area.x })
  }
}