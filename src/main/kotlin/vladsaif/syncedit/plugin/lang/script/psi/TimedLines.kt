package vladsaif.syncedit.plugin.lang.script.psi

import vladsaif.syncedit.plugin.IRange

data class TimedLines(val lines: IRange, val time: IRange) : Comparable<TimedLines> {
  override fun compareTo(other: TimedLines) = CMP.compare(this, other)

  companion object {
    private val CMP = compareBy<TimedLines>({ it.lines }, { it.time })
  }
}

