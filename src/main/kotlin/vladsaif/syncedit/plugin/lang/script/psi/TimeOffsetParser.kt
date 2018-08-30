package vladsaif.syncedit.plugin.lang.script.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import vladsaif.syncedit.plugin.IRange

object TimeOffsetParser {

  /**
   * Parse 'timeOffset(Long)' statements in [psiFile].
   *
   * @param psiFile file of GUI test script.
   * @return mapping of extracted time ranges to line range that lies in it.
   */
  fun parse(psiFile: KtFile): List<TimedLines> {
    if (PsiTreeUtil.hasErrorElements(psiFile)) throw IllegalArgumentException("File must not contain error elements.")
    val expressionsLines = mutableListOf<IRange>()
    val absoluteTimeOffsets = mutableListOf<TimeOffset>()
    val document = psiFile.viewProvider.document
        ?: throw AssertionError("Events are not enabled for $psiFile")
    var accumulator = 0
    BlockVisitor.visit(psiFile) {
      if (isTimeOffset(it)) {
        val line = document.getLineNumber(it.textOffset)
        val offsetValue = parseOffset(it.text)
        absoluteTimeOffsets.add(TimeOffset(line, offsetValue + accumulator))
        accumulator += offsetValue
      } else {
        val start = document.getLineNumber(it.textOffset)
        val end = document.getLineNumber(it.textOffset + it.textLength)
        expressionsLines.add(IRange(start, end))
      }
    }
    // Add initial (before whole script) timeOffset statement
    absoluteTimeOffsets.add(0, TimeOffset(-1, 0))
    return constructTimedStatements(expressionsLines, absoluteTimeOffsets)
  }

  /** This operation can be implemented easily with O(expressions.size * offsets.size) complexity.
   *
   * But it will mostly called from EDT so it worth to write some nasty code to achieve
   * O(min(expressions.size, offsets.size)) complexity in worst case.
   *
   * @param expressions sorted.
   * @param offsets sorted.
   **/
  private fun constructTimedStatements(expressions: List<IRange>, offsets: List<TimeOffset>): List<TimedLines> {
    val timedStatements = mutableListOf<TimedLines>()
    val intervals = offsets.sortedBy { it.line }
    var j = 0
    out@ for (expr in expressions) {
      while (j < intervals.size) {
        val interval = intervals[j]
        if (interval.line <= expr.start) {
          while (j < intervals.size && intervals[j].line <= expr.start) j++
          j--
          var i = j + 1
          while (i < intervals.size) {
            if (expr.end <= intervals[i].line) {
              timedStatements.add(
                  TimedLines(lines = expr, time = IRange(intervals[j].timeOffset, intervals[i].timeOffset))
              )
              continue@out
            }
            i++
          }
          break
        }
        j++
      }
      timedStatements.add(TimedLines(lines = expr, time = IRange.EMPTY_RANGE))
    }
    return timedStatements
  }

  private val ourTimeOffsetArgumentsRegex = "(ms=)?[0-9]+L".toRegex()

  fun isTimeOffset(psiElement: PsiElement): Boolean {
    val element = if (psiElement is KtScriptInitializer) psiElement.children.firstOrNull() else psiElement
    (element as? KtCallExpression)?.let {
      val reference = it.referenceExpression() ?: return false
      if (reference.text != "timeOffset") return false
      val arguments = it.valueArguments
      return arguments.size == 1
          && ourTimeOffsetArgumentsRegex.matches(arguments.first().text.replace("\\s+".toRegex(), ""))
    }
    return false
  }

  fun parseOffset(string: String): Int {
    return string.filter { it.isDigit() }.toInt()
  }
}