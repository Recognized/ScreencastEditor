package vladsaif.syncedit.plugin.lang.script.psi

import gnu.trove.TObjectIntHashMap
import vladsaif.syncedit.plugin.editor.audioview.waveform.ChangeNotifier
import vladsaif.syncedit.plugin.editor.audioview.waveform.impl.DefaultChangeNotifier
import vladsaif.syncedit.plugin.util.empty
import vladsaif.syncedit.plugin.util.end

class CodeBlockModel(blocks: List<CodeBlock>) : ChangeNotifier by DefaultChangeNotifier() {
  private val myDepthCache = TObjectIntHashMap<CodeBlock>()

  var blocks: List<CodeBlock> = listOf()
    set(value) {
      field = value.asSequence()
          .filter { !it.timeRange.empty }
          .sortedBy { it.timeRange.start }
          .toList()
      myDepthCache.clear()
      recalculateDepth(field)
      fireStateChanged()
    }

  init {
    this.blocks = blocks
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as CodeBlockModel

    if (blocks != other.blocks) return false

    return true
  }

  override fun hashCode(): Int {
    return blocks.hashCode()
  }

  override fun toString(): String {
    return blocks.joinToString(separator = "")
  }

  fun findDepth(block: CodeBlock): Int {
    return if (myDepthCache.containsKey(block)) myDepthCache[block] else -1
  }

  fun findAdjacentBorders(time: Int): Pair<Int, Int> {
    val borders = mutableListOf<Int>()
    borders.addAll(blocks.map { it.timeRange.start })
    borders.addAll(blocks.asSequence().filter { it.isBlock }.map { it.timeRange.end })
    borders.remove(time)
    var closestLeft = -1
    var closestRight = -1
    var distanceLeft = Int.MAX_VALUE
    var distanceRight = Int.MAX_VALUE
    for (border in borders) {
      if (border < time && time - border < distanceLeft) {
        distanceLeft = time - border
        closestLeft = border
      }
      if (border >= time && border - time < distanceRight) {
        distanceRight = border - time
        closestRight = border
      }
    }
    return closestLeft to closestRight
  }

  private fun recalculateDepth(currentBlocks: List<CodeBlock>, currentDepth: Int = 0) {
    for (block in currentBlocks) {
      myDepthCache.put(block, currentDepth)
      recalculateDepth(block.innerBlocks, currentDepth + 1)
    }
  }
}

class CodeBlockBuilder {
  private val myBlocks = mutableListOf<CodeBlock>()

  fun block(code: String, timeRange: IntRange, closure: CodeBlockBuilder.() -> Unit) {
    val builder = CodeBlockBuilder()
    builder.closure()
    myBlocks.add(CodeBlock(code, timeRange, true, builder.build()))
  }

  fun statement(code: String, timeRange: IntRange) {
    myBlocks.add(CodeBlock(code, timeRange, false))
  }

  fun build(): List<CodeBlock> {
    return myBlocks
  }
}

fun codeBlockModel(closure: CodeBlockBuilder.() -> Unit): CodeBlockModel {
  val builder = CodeBlockBuilder()
  builder.closure()
  return CodeBlockModel(builder.build())
}