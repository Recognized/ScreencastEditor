package vladsaif.syncedit.plugin.lang.script.psi

import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.psi.KtFile
import vladsaif.syncedit.plugin.IRange

object TimeOffsetParser {

  private val myTimeOffsetRegex: Regex = """timeOffset\((ms=)?[0-9]+L\)""".toRegex()

  /**
   * Parse 'timeOffset(Long)' statements in [psiFile].
   *
   * @param psiFile file of GUI test script.
   * @return mapping of extracted time ranges to line range that lies in it.
   */
  fun parse(psiFile: KtFile): Map<IRange, IRange> {
    val text = PsiDocumentManager.getInstance(psiFile.project)
        .getDocument(psiFile)!!
        .text

    return parseText(text)
  }

  internal fun parseText(text: CharSequence): Map<IRange, IRange> {
    val lines = text.split('\n').toMutableList()
    val offsetsToLines = getOffsetToLineList(lines).toMutableList()
    offsetsToLines.firstOrNull()?.let {
      if (it.second != 0) {
        offsetsToLines.add(0, 0 to 0)
      }
    }
    return offsetsToLines.zipWithNext()
        .map { (start, end) -> IRange(start.first, end.first) to IRange(start.second + 1, end.second - 1) }
        .filter { (_, lineRange) -> !lineRange.empty }
        .toMap()
  }

  internal fun getOffsetToLineList(lines: List<String>): List<Pair<Int, Int>> {
    return lines.mapIndexed { index, str -> str to index }
        .filter { (line, _) -> isTimeOffset(line) }
        .map { (offset, index) -> parseOffset(offset) to index }
  }

  internal fun isTimeOffset(string: String): Boolean {
    return myTimeOffsetRegex.matches(string.filter { !it.isWhitespace() })
  }

  internal fun parseOffset(string: String): Int {
    return string.filter { it.isDigit() }.toInt()
  }
}