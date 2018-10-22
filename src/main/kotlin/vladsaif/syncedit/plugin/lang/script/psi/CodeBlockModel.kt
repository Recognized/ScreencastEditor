package vladsaif.syncedit.plugin.lang.script.psi

import gnu.trove.TObjectIntHashMap
import vladsaif.syncedit.plugin.editor.audioview.waveform.ChangeNotifier
import vladsaif.syncedit.plugin.editor.audioview.waveform.impl.DefaultChangeNotifier
import vladsaif.syncedit.plugin.util.empty

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