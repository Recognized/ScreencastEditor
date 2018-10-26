package vladsaif.syncedit.plugin.lang.script.psi

import com.intellij.application.options.CodeStyle
import gnu.trove.TObjectIntHashMap
import org.jetbrains.kotlin.idea.KotlinFileType
import vladsaif.syncedit.plugin.actions.times
import vladsaif.syncedit.plugin.editor.audioview.waveform.ChangeNotifier
import vladsaif.syncedit.plugin.editor.audioview.waveform.impl.DefaultChangeNotifier
import vladsaif.syncedit.plugin.util.empty
import vladsaif.syncedit.plugin.util.end

class CodeModel(blocks: List<Code>) : ChangeNotifier by DefaultChangeNotifier() {
  private val myDepthCache = TObjectIntHashMap<Code>()

  var blocks: List<Code> = listOf()
    set(value) {
      field = Code.mergeWithSameRange(value.asSequence()
          .filter { it !is Block || !it.timeRange.empty }
          .sortedBy { it.startTime }
          .toList())
      myDepthCache.clear()
      recalculateDepth(field)
      fireStateChanged()
    }

  init {
    this.blocks = blocks
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as CodeModel
    if (blocks != other.blocks) return false
    return true
  }

  override fun hashCode(): Int {
    return blocks.hashCode()
  }

  override fun toString(): String {
    return blocks.joinToString(separator = "")
  }

  fun findDepth(code: Code): Int {
    return if (myDepthCache.containsKey(code)) myDepthCache[code] else -1
  }

  fun replace(oldCode: Code, newCode: Code) {
    val (changed, newBlocks) = replaceImpl(blocks, oldCode, newCode)
    if (changed) {
      blocks = newBlocks
    }
  }

  private fun replaceImpl(currentLevel: List<Code>, oldCode: Code, newCode: Code): Pair<Boolean, List<Code>> {
    val list = mutableListOf<Code>()
    var somethingChanged = false
    for (code in currentLevel) {
      if (code === oldCode) {
        somethingChanged = true
        list.add(newCode)
      } else {
        when (code) {
          is Statement -> list.add(code)
          is Block -> {
            val (changed, newInner) = replaceImpl(code.innerBlocks, oldCode, newCode)
            if (changed) {
              list.add(Block(code.code, code.timeRange, newInner))
              somethingChanged = true
            } else {
              list.add(code)
            }
          }
        }
      }
    }
    return somethingChanged to if (somethingChanged) list else currentLevel
  }

  fun serialize(): String {
    return buildString {
      val indent = " " * CodeStyle.getDefaultSettings().getIndentOptions(KotlinFileType.INSTANCE).INDENT_SIZE
      var lastOffset = 0
      for (code in blocks) {
        lastOffset = serializeImpl(this@buildString, code, lastOffset, indent)
      }
    }
  }

  private fun serializeImpl(
      builder: StringBuilder,
      code: Code,
      lastOffset: Int,
      indent: String,
      indentation: Int = 0
  ): Int {
    with(builder) {
      var localLastOffset = lastOffset
      append(indent * indentation)
      append(TimeOffsetParser.createTimeOffset(code.startTime - localLastOffset))
      localLastOffset = code.startTime
      append("\n")
      when (code) {
        is Statement -> append(indent * indentation + code.code + "\n")
        is Block -> {
          append(indent * indentation + code.code + " {\n")
          for (inner in code.innerBlocks) {
            localLastOffset = serializeImpl(builder, inner, localLastOffset, indent, indentation + 1)
          }
          append(indent * indentation + "}\n")
          append(indent * indentation + TimeOffsetParser.createTimeOffset(code.endTime - localLastOffset) + "\n")
          localLastOffset = code.endTime
        }
      }
      return localLastOffset
    }
  }

  private fun findParent(ofThis: Code, currentParent: Block? = null): Block? {
    val amongThem = currentParent?.innerBlocks ?: blocks
    for (block in amongThem) {
      if (block === ofThis) {
        return currentParent
      }
    }
    for (block in amongThem.filterIsInstance(Block::class.java)) {
      val result = findParent(ofThis, block)
      if (result != null) return result
    }
    return null
  }

  fun findDragBoundary(beingFind: Statement) = findDragBoundary(beingFind, true)

  fun findDragBoundary(beingFind: Block, isLeft: Boolean) = findDragBoundary(beingFind as Code, isLeft)

  private fun findDragBoundary(beingFind: Code, isLeft: Boolean): Pair<Int, Int> {
    val parent = findParent(beingFind)
    val parentBlocks = parent?.innerBlocks ?: blocks
    val index = parentBlocks.asSequence().withIndex().find { (_, x) -> x == beingFind }!!.index
    val outerBoundaryLeft = if (index > 0) {
      parentBlocks[index - 1].endTime
    } else {
      parent?.timeRange?.start ?: -1
    }
    val outerBoundaryRight = if (index + 1 < parentBlocks.size) {
      parentBlocks[index + 1].startTime
    } else {
      parent?.timeRange?.end ?: -1
    }
    if (beingFind !is Block) return outerBoundaryLeft to outerBoundaryRight
    val innerBoundary = if (isLeft) {
      beingFind.innerBlocks.firstOrNull()?.startTime ?: beingFind.timeRange.end
    } else {
      beingFind.innerBlocks.asReversed().firstOrNull()?.endTime ?: beingFind.timeRange.start
    }
    return if (isLeft) {
      outerBoundaryLeft to innerBoundary
    } else {
      innerBoundary to outerBoundaryRight
    }
  }

  private fun recalculateDepth(codeList: List<Code>, currentDepth: Int = 0) {
    for (code in codeList) {
      myDepthCache.put(code, currentDepth)
      if (code is Block) {
        recalculateDepth(code.innerBlocks, currentDepth + 1)
      }
    }
  }
}

class CodeBlockBuilder {
  private val myBlocks = mutableListOf<Code>()

  fun block(code: String, timeRange: IntRange, closure: CodeBlockBuilder.() -> Unit) {
    val builder = CodeBlockBuilder()
    builder.closure()
    myBlocks.add(Block(code, timeRange, builder.build()))
  }

  fun statement(code: String, startOffset: Int) {
    myBlocks.add(Statement(code, startOffset))
  }

  fun build(): List<Code> {
    return myBlocks
  }
}

fun codeBlockModel(closure: CodeBlockBuilder.() -> Unit): CodeModel {
  val builder = CodeBlockBuilder()
  builder.closure()
  return CodeModel(builder.build())
}