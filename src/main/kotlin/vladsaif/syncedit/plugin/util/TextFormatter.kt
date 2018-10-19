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
}