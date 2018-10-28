package vladsaif.syncedit.plugin.lang.script.psi

import vladsaif.syncedit.plugin.actions.times
import vladsaif.syncedit.plugin.util.empty
import vladsaif.syncedit.plugin.util.end

sealed class Code(val code: String) {
  val startTime
    get() = when (this) {
      is Block -> timeRange.start
      is Statement -> timeOffset
    }

  val endTime
    get() = when (this) {
      is Block -> timeRange.end
      is Statement -> timeOffset
    }

  abstract fun toScript(builder: StringBuilder, indentation: Int = 0)

  final override fun toString(): String {
    return buildString {
      toScript(this@buildString)
    }
  }

  companion object {
    const val indentationUnit = "  "

    @JvmStatic
    fun mergeWithSameRange(innerBlocks: List<Code>): List<Code> {
      val list = mutableListOf<Code>()
      for (block in innerBlocks) {
        if (!list.isEmpty() && block.startTime == list.last().startTime) {
          val newCode = list.last().code + "\n" + block.code
          val captured = list.last()
          val newEnd = captured.endTime
          list[list.size - 1] = when {
            block is Statement && captured is Statement -> Statement(newCode, newEnd)
            else -> Block(newCode, block.startTime..newEnd,
                (block as? Block)?.innerBlocks ?: listOf<Code>()+
                ((captured as? Block)?.innerBlocks ?: listOf())
            )
          }
        } else {
          list.add(block)
        }
      }
      return list
    }
  }
}

class Statement(code: String, val timeOffset: Int) : Code(code) {

  override fun toScript(builder: StringBuilder, indentation: Int) {
    builder.append(indentationUnit * indentation
        + code.split("\n").joinToString("\n" + "  " * indentation))
    builder.append("  $timeOffset\n")
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Statement

    if (code != other.code) return false
    if (timeOffset != other.timeOffset) return false

    return true
  }

  override fun hashCode(): Int {
    var result = code.hashCode()
    result = 31 * result + timeOffset
    return result
  }
}

class Block(
    code: String,
    val timeRange: IntRange,
    innerBlocks: List<Code> = listOf()
) : Code(code) {

  val innerBlocks = mergeWithSameRange(innerBlocks.asSequence()
      .filter { it !is Block || !it.timeRange.empty }
      .sortedBy { it.startTime }
      .toList()
  )

  override fun toScript(builder: StringBuilder, indentation: Int) {
    builder.append(indentationUnit * indentation + code.split("\n").joinToString("\n" + "  " * indentation))
    builder.append(" {\n")
    for (block in innerBlocks) {
      block.toScript(builder, indentation + 1)
    }
    builder.append("  " * indentation + "} $timeRange\n")
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Block

    if (code != other.code) return false
    if (timeRange != other.timeRange) return false
    if (innerBlocks != other.innerBlocks) return false

    return true
  }

  override fun hashCode(): Int {
    var result = code.hashCode()
    result = 31 * result + timeRange.hashCode()
    result = 31 * result + innerBlocks.hashCode()
    return result
  }
}