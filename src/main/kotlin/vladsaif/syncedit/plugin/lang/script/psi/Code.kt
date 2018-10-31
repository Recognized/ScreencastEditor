package vladsaif.syncedit.plugin.lang.script.psi

import vladsaif.syncedit.plugin.actions.times
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
    const val INDENTATION_UNIT = "  "
  }
}

class Statement(code: String, val timeOffset: Int) : Code(code) {

  override fun toScript(builder: StringBuilder, indentation: Int) {
    builder.append(
      INDENTATION_UNIT * indentation
          + code.split("\n").joinToString("\n" + "  " * indentation)
    )
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

  val innerBlocks = innerBlocks.sortedBy { it.startTime }

  override fun toScript(builder: StringBuilder, indentation: Int) {
    builder.append(INDENTATION_UNIT * indentation + code.split("\n").joinToString("\n" + "  " * indentation))
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