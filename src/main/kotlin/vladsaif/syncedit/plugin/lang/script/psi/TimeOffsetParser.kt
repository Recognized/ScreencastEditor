package vladsaif.syncedit.plugin.lang.script.psi

import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.editor.fixers.end
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.psi.psiUtil.referenceExpression
import vladsaif.syncedit.plugin.util.end
import vladsaif.syncedit.plugin.util.length

object TimeOffsetParser {
  private val TIME_RANGE_KEY = Key.create<IntRange>("INT-RANGE-KEY")
  private val FRACTION_KEY = Key.create<Pair<Int, Int>>("FRACTION-KEY")
  private val TIME_OFFSET_ARGUMENT_REGEX = "(ms=)?[0-9]+L".toRegex()

  fun parse(psiFile: KtFile): CodeModel {
    if (PsiTreeUtil.hasErrorElements(psiFile)) throw IllegalArgumentException("File must not contain error element.")
    val psiElements = mutableListOf<PsiElement>()
    val absoluteTimeOffsets = mutableListOf<TimeOffset>()
    var accumulator = 0
    val clone = psiFile.copy() as KtFile
    BlockVisitor.visit(clone) {
      if (isTimeOffset(it)) {
        val offsetValue = parseOffset(it.text)
        absoluteTimeOffsets.add(TimeOffset(it.textOffset, offsetValue + accumulator))
        accumulator += offsetValue
      } else {
        psiElements.add(it)
      }
    }
    // Add initial (before whole script) timeOffset statement
    absoluteTimeOffsets.add(0, TimeOffset(-1, 0))
    // Add last offset implicitly
    absoluteTimeOffsets.add(TimeOffset(clone.textLength * 2, absoluteTimeOffsets.last().timeOffset))
    markElements(psiElements, absoluteTimeOffsets)
    val blocks = BlockVisitor.fold(clone) { element, list: List<Code>, isBlock ->
      val range = element.getUserData(TIME_RANGE_KEY)!!
      element.putUserData(TIME_RANGE_KEY, null)
      val (k, total) = element.getUserData(FRACTION_KEY)!!
      element.putUserData(FRACTION_KEY, null)
      val correctedRange = (range.start + range.length / total * (k - 1))..range.end
      if (isBlock) {
        Block(
          element.text.substringBefore("{").trim { it.isWhitespace() },
          correctedRange,
          list
        )
      } else {
        Statement(element.text, correctedRange.start)
      }
    }
    return CodeModel(blocks)
  }

  /**
   * This operation can be implemented easily with O(expressions.size * offsets.size) complexity.
   *
   * But it will mostly called from EDT so it worth to write some nasty code to achieve
   * O(min(expressions.size, offsets.size)) complexity in worst case.
   *
   * @param expressions sorted.
   * @param offsets sorted.
   **/
  private fun markElements(
    expressions: List<PsiElement>,
    offsets: List<TimeOffset>
  ) {
    val intervals = offsets.sortedBy { it.textOffset }
    var j = 0
    val sameRangeElements = mutableMapOf<IntRange, MutableList<PsiElement>>()
    out@ for (expr in expressions) {
      while (j < intervals.size) {
        val interval = intervals[j]
        if (interval.textOffset <= expr.textOffset) {
          while (j < intervals.size && intervals[j].textOffset <= expr.textOffset) j++
          j--
          var i = j + 1
          while (i < intervals.size) {
            if (expr.textRange.end <= intervals[i].textOffset) {
              val range = intervals[j].timeOffset..intervals[i].timeOffset
              expr.putUserData(TIME_RANGE_KEY, range)
              sameRangeElements.computeIfAbsent(range) { mutableListOf() }.add(expr)
              continue@out
            }
            i++
          }
          break
        }
        j++
      }
      expr.putUserData(TIME_RANGE_KEY, IntRange.EMPTY)
    }
    for (list in sameRangeElements.values) {
      for ((index, expr) in list.withIndex()) {
        expr.putUserData(FRACTION_KEY, index + 1 to list.size)
      }
    }
  }

  fun isTimeOffset(psiElement: PsiElement): Boolean {
    val element = if (psiElement is KtScriptInitializer) psiElement.children.firstOrNull() else psiElement
    (element as? KtCallExpression)?.let {
      val reference = it.referenceExpression() ?: return false
      if (reference.text != "timeOffset") return false
      val arguments = it.valueArguments
      return arguments.size == 1
          && TIME_OFFSET_ARGUMENT_REGEX.matches(arguments.first().text.replace("\\s+".toRegex(), ""))
    }
    return false
  }

  fun parseOffset(string: String): Int {
    return string.filter { it.isDigit() }.toInt()
  }

  fun createTimeOffset(time: Int): String {
    return "timeOffset(ms = ${time}L)"
  }

  private data class TimeOffset(val textOffset: Int, val timeOffset: Int)
}