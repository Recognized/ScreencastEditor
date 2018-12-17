package com.github.recognized.screencast.editor.lang.script.psi

import com.github.recognized.screencast.recorder.times

sealed class Code(val code: String) {
  val startTime
    get() = when (this) {
      is Block -> timeRange.start
      is Statement -> timeOffset
    }

  val endTime
    get() = when (this) {
      is Block -> timeRange.endInclusive
      is Statement -> timeOffset
    }

  abstract fun toScript(builder: StringBuilder, indentation: Int = 0)

  abstract fun labelEquals(other: Code): Boolean

  final override fun toString(): String {
    return buildString {
      toScript(this@buildString)
    }
  }

  fun <T> fold(fnSt: (Statement) -> T, fnBl: (Block, List<T>) -> T): T {
    return when (this) {
      is Statement -> fnSt(this)
      is Block -> fnBl(this, innerBlocks.map { it.fold(fnSt, fnBl) })
    }
  }

  companion object {
    const val INDENTATION_UNIT = "  "
  }
}

fun List<Code>.labelEquals(other: List<Code>): Boolean {
  return this.size == other.size &&
      this.zip(other).all { (a, b) -> a.labelEquals(b) }
}

class Statement(code: String, val timeOffset: Int) : Code(code) {

  override fun toScript(builder: StringBuilder, indentation: Int) {
    builder.append(
      INDENTATION_UNIT * indentation
          + code.split("\n").joinToString("\n" + "  " * indentation)
    )
    builder.append("  $timeOffset\n")
  }

  fun copy(code: String = this.code, offset: Int = this.timeOffset) = Statement(code, offset)

  override fun labelEquals(other: Code): Boolean {
    return other is Statement && other.code == this.code
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

  fun copy(
    code: String = this.code,
    range: IntRange = this.timeRange,
    innerBlocks: List<Code> = this.innerBlocks
  ) = Block(code, range, innerBlocks)

  override fun labelEquals(other: Code): Boolean {
    return other is Block &&
        other.code == this.code && this.innerBlocks.labelEquals(other.innerBlocks)
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