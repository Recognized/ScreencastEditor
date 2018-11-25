package vladsaif.syncedit.plugin.util

object TextFormatter {

  /**
   * Split text so every line fits in [lineSize] or contains only one word (which cannot be fit).
   *
   * @param getWidth function of transformation string into size units.
   */
  fun splitText(text: String, lineSize: Int, getWidth: (String) -> Int = { it.length }): List<String> {
    val words = text.split("\\s+".toRegex())
    return formatLines(words, lineSize, getWidth)
  }

  /**
   * Fit [line] in [lineSize] units.
   *
   * @param getWidth function of transformation string into size units.
   */
  fun createEllipsis(line: String, lineSize: Int, getWidth: (String) -> Int = { it.length }): String {
    return if (getWidth(line) > lineSize) {
      var dropped = line
      while (!dropped.isEmpty() && getWidth(dropped) > lineSize) {
        dropped = when (dropped.length) {
          1 -> ""
          2 -> "."
          3 -> ""
          else -> dropped.replaceRange(dropped.length - 4, dropped.length, "...")
        }
      }
      dropped
    } else {
      line
    }
  }

  fun formatLines(
    words: List<String>,
    lineSize: Int,
    getWidth: (String) -> Int = { it.length },
    separator: Char = ' '
  ): List<String> {
    val lines = mutableListOf<String>()
    var builder = StringBuilder()
    for (word in words) {
      if (getWidth(builder.toString() + word) > lineSize) {
        if (builder.isEmpty()) {
          lines.add(word)
          continue
        }
        lines.add(builder.toString().trim())
        builder = StringBuilder()
      }
      builder.append(word)
      builder.append(separator)
    }
    builder.toString().let {
      if (!it.isEmpty()) {
        lines.add(it.trim(separator))
      }
    }
    return lines
  }

  fun formatTime(ns: Long) = buildString {
    if (ns < 0) throw IllegalArgumentException()
    val minutes = ns / (60 * 1_000_000_000L)
    val seconds = (ns / 1_000_000_000L) % 60
    val nanos = ns % 1_000_000_000L
    if (minutes != 0L) {
      append("$minutes:")
      if (seconds < 10L) {
        append("0")
      }
    }
    append("$seconds.")
    when {
      nanos < 10 -> append("00000000")
      nanos < 100 -> append("0000000")
      nanos < 1000 -> append("000000")
      nanos < 10000 -> append("00000")
      nanos < 100000 -> append("0000")
      nanos < 1000000 -> append("000")
      nanos < 10000000 -> append("00")
      nanos < 100000000 -> append("0")
    }
    append(nanos)
  }
}