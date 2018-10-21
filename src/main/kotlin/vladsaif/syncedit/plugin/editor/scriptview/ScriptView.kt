package vladsaif.syncedit.plugin.editor.scriptview

import com.intellij.ui.components.JBPanel
import gnu.trove.THashMap
import gnu.trove.TObjectHashingStrategy
import vladsaif.syncedit.plugin.editor.scriptview.ScriptGraphics.CODE_BLOCK_BACKGROUND
import vladsaif.syncedit.plugin.lang.script.psi.CodeBlock
import vladsaif.syncedit.plugin.model.ScreencastFile
import vladsaif.syncedit.plugin.util.*
import java.awt.*
import java.util.*
import java.util.concurrent.TimeUnit
import javax.swing.event.ChangeListener
import kotlin.math.max

class ScriptView(val screencast: ScreencastFile, givenCoordinator: Coordinator?) : JBPanel<ScriptView>() {
  val coordinator = givenCoordinator ?: LinearCoordinator().apply {
    setTimeUnitsPerPixel(50, TimeUnit.MILLISECONDS)
  }

  init {
    screencast.codeBlockModel.addChangeListener(ChangeListener {
      revalidate()
      repaint()
    })
  }

  private val myShortenedCode = THashMap<CodeBlock, String>(object : TObjectHashingStrategy<CodeBlock> {
    override fun equals(p0: CodeBlock?, p1: CodeBlock?): Boolean {
      return p0?.timeRange == p1?.timeRange && p0?.code == p1?.code
    }

    override fun computeHashCode(p0: CodeBlock): Int {
      return Objects.hash(p0.timeRange, p0.code)
    }
  })

  override fun paint(g: Graphics) {
    val bounds = Rectangle(0, 0, width, height)
    val timeBounds = coordinator.toNanoseconds(bounds.x..(bounds.x + bounds.width))
    with(g as Graphics2D) {
      val metrics = getFontMetrics(ScriptGraphics.FONT)
      font = ScriptGraphics.FONT
      val getLength = { string: String -> metrics.stringWidth(string) }
      for (codeBlock in screencast.codeBlockModel.blocks) {
        if (codeBlock.timeRange.msToNs().intersects(timeBounds)) {
          drawCodeBlock(codeBlock, bounds, getLength, metrics, CODE_BLOCK_BACKGROUND)
        }
      }
    }
  }

  private fun Graphics2D.drawCodeBlock(
      codeBlock: CodeBlock,
      bounds: Rectangle,
      getLength: (String) -> Int,
      metrics: FontMetrics,
      backgroundColor: Color
  ) {
    val screenRange = coordinator.toScreenPixel(codeBlock.timeRange.msToNs(), TimeUnit.NANOSECONDS)
    val innerRange = screenRange.padded(ScriptGraphics.PADDING.toInt())
    color = backgroundColor
    fillRect(screenRange.start, bounds.y, screenRange.length, bounds.height)
    stroke = ScriptGraphics.BORDER_STROKE
    color = ScriptGraphics.CODE_BLOCK_BORDER
    drawLine(screenRange.start, bounds.y, screenRange.start, bounds.y + bounds.height)
    drawLine(screenRange.end, bounds.y, screenRange.end, bounds.y + bounds.height)
    val string = myShortenedCode.getOrPut(codeBlock) {
      TextFormatter.createEllipsis(codeBlock.code, innerRange.length, getLength)
    }
    color = ScriptGraphics.FONT_COLOR
    drawString(string, innerRange.start, bounds.y + metrics.height + ScriptGraphics.PADDING.toInt())
    val newBounds = Rectangle(
        bounds.x,
        bounds.y + metrics.height * 2 + ScriptGraphics.PADDING.toInt(),
        bounds.width,
        max(bounds.height - metrics.height * 2 - ScriptGraphics.PADDING.toInt(), 0)
    )
    if (newBounds.height != 0) {
      val newColor = ScriptGraphics.rotate(backgroundColor)
      for (innerBlock in codeBlock.innerBlocks) {
        drawCodeBlock(innerBlock, newBounds, getLength, metrics, newColor)
      }
    }
  }
}