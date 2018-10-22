package vladsaif.syncedit.plugin.lang.script.psi

import vladsaif.syncedit.plugin.actions.times
import vladsaif.syncedit.plugin.util.empty

open class CodeBlock(
    val code: String,
    val timeRange: IntRange,
    val isBlock: Boolean,
    innerBlocks: List<CodeBlock> = listOf()
) {

  val innerBlocks = mergeWithSameRange(innerBlocks.asSequence()
      .filter { !it.timeRange.empty }
      .sortedBy { it.timeRange.start }
      .toList()
  )

  private fun toScript(builder: StringBuilder, indentation: Int = 0) {
    builder.append("  " * indentation + code.split("\n").joinToString(separator = "\n" + "  " * indentation))
    println(innerBlocks.size)
    if (isBlock) {
      builder.append(" {\n")
      for (block in innerBlocks) {
        block.toScript(builder, indentation + 1)
      }
      builder.append("  " * indentation + "} $timeRange\n")
    } else {
      builder.append(" $timeRange\n")
    }
  }

  override fun toString() = buildString {
    toScript(this)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as CodeBlock

    if (code != other.code) return false
    if (timeRange != other.timeRange) return false
    if (isBlock != other.isBlock) return false
    if (innerBlocks != other.innerBlocks) return false

    return true
  }

  override fun hashCode(): Int {
    var result = code.hashCode()
    result = 31 * result + timeRange.hashCode()
    result = 31 * result + isBlock.hashCode()
    result = 31 * result + innerBlocks.hashCode()
    return result
  }

  companion object {

    private fun mergeWithSameRange(innerBlocks: List<CodeBlock>): List<CodeBlock> {
      val list = mutableListOf<CodeBlock>()
      for (block in innerBlocks) {
        if (!list.isEmpty() && block.timeRange == list.last().timeRange) {
          list[list.size - 1] = CodeBlock(
              list.last().code + "\n" + block.code,
              block.timeRange,
              list.last().isBlock || block.isBlock,
              list.last().innerBlocks + block.innerBlocks
          )
        } else {
          list.add(block)
        }
      }
      return list
    }
  }
}