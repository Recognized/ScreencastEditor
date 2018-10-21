package vladsaif.syncedit.plugin.lang.script.psi

import vladsaif.syncedit.plugin.editor.audioview.waveform.ChangeNotifier
import vladsaif.syncedit.plugin.editor.audioview.waveform.impl.DefaultChangeNotifier
import vladsaif.syncedit.plugin.util.empty

class CodeBlockModel(blocks: List<CodeBlock>) : ChangeNotifier by DefaultChangeNotifier() {

  var blocks: List<CodeBlock> = blocks.asSequence()
      .filter { !it.timeRange.empty }
      .sortedBy { it.timeRange.start }
      .toList()
  set(value) {
    field = value
    fireStateChanged()
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
}

class CodeBlockBuilder {
  private val myBlocks = mutableListOf<CodeBlock>()

  fun block(code: String, timeRange: IntRange, closure: CodeBlockBuilder.() -> Unit) {
    val builder = CodeBlockBuilder()
    builder.closure()
    myBlocks.add(CodeBlock(code, timeRange, builder.build()))
  }

  fun statement(code: String, timeRange: IntRange) {
    myBlocks.add(CodeBlock(code, timeRange))
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