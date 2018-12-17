package com.github.recognized.screencast.editor.lang.script.psi

import com.github.recognized.kotlin.ranges.extensions.floorToInt
import com.github.recognized.kotlin.ranges.extensions.shift
import com.github.recognized.screencast.recorder.times
import com.github.tmatek.zhangshasha.TreeDistance
import com.github.tmatek.zhangshasha.TreeNode
import com.intellij.openapi.diagnostic.logger
import gnu.trove.TObjectIntHashMap
import org.jetbrains.kotlin.psi.KtFile
import java.util.concurrent.TimeUnit

interface CodeModelView {
  val codes: List<Code>
  fun findDepth(code: Code): Int
  fun serialize(): String
  fun findDragBoundary(beingFind: Statement): Pair<Int, Int>?
  fun findDragBoundary(beingFind: Block, isLeft: Boolean): Pair<Int, Int>?
  fun createTextWithoutOffsets(): MarkedText
  fun createEditableTree(): RawTreeNode
  fun transformedByScript(ktFile: KtFile): CodeModel
}

class CodeModel(blocks: List<Code>) : CodeModelView {
  private val myDepthCache = TObjectIntHashMap<Code>()

  override var codes: List<Code> = listOf()
    set(value) {
      field = value.sortedBy { it.startTime }
      myDepthCache.clear()
      recalculateDepth(field)
    }

  init {
    this.codes = blocks
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as CodeModel
    if (codes != other.codes) return false
    return true
  }

  override fun hashCode(): Int {
    return codes.hashCode()
  }

  override fun toString(): String {
    return codes.joinToString(separator = "")
  }

  override fun findDepth(code: Code): Int {
    return if (myDepthCache.containsKey(code)) myDepthCache[code] else -1
  }

  fun replace(oldCode: Code, newCode: Code) {
    val (changed, newBlocks) = replaceImpl(codes, oldCode, newCode)
    if (changed) {
      codes = newBlocks
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

  override fun serialize(): String {
    val builder = MarkedTextBuilder(true)
    var lastOffset = 0
    for (code in codes) {
      lastOffset = serializeImpl(builder, code, lastOffset)
    }
    return builder.done().text
  }

  override fun createTextWithoutOffsets(): MarkedText {
    val builder = MarkedTextBuilder(false)
    var lastOffset = 0
    for (code in codes) {
      lastOffset = serializeImpl(builder, code, lastOffset)
    }
    return builder.done()
  }

  private fun serializeImpl(
    builder: MarkedTextBuilder,
    code: Code,
    lastOffset: Int,
    indentation: Int = 0
  ): Int {
    with(builder) {
      var localLastOffset = lastOffset
      val indent = Code.INDENTATION_UNIT * indentation
      appendOffset(indentation, code.startTime - localLastOffset)
      localLastOffset = code.startTime
      when (code) {
        is Statement -> {
          append(indent)
          append(code.code)
          append("\n")
        }
        is Block -> {
          append(indent)
          append(code.code)
          append(" {\n")
          for (inner in code.innerBlocks) {
            localLastOffset = serializeImpl(builder, inner, localLastOffset, indentation + 1)
          }
          append("$indent}\n")
          appendOffset(indentation, code.endTime - localLastOffset)
          localLastOffset = code.endTime
        }
      }
      return localLastOffset
    }
  }

  private fun findParent(ofThis: Code, currentParent: Block? = null): Block? {
    val amongThem = currentParent?.innerBlocks ?: codes
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

  override fun findDragBoundary(beingFind: Statement) = findDragBoundary(beingFind, true)

  override fun findDragBoundary(beingFind: Block, isLeft: Boolean) = findDragBoundary(beingFind as Code, isLeft)

  private fun findDragBoundary(beingFind: Code, isLeft: Boolean): Pair<Int, Int>? {
    val parent = findParent(beingFind)
    val parentBlocks = parent?.innerBlocks ?: codes
    val index = parentBlocks.asSequence().withIndex().find { (_, x) -> x == beingFind }?.index ?: return null
    val outerBoundaryLeft = if (index > 0) {
      parentBlocks[index - 1].endTime
    } else {
      parent?.timeRange?.start ?: -1
    }
    val outerBoundaryRight = if (index + 1 < parentBlocks.size) {
      parentBlocks[index + 1].startTime
    } else {
      parent?.timeRange?.endInclusive ?: -1
    }
    if (beingFind !is Block) return outerBoundaryLeft to outerBoundaryRight
    val innerBoundary = if (isLeft) {
      beingFind.innerBlocks.firstOrNull()?.startTime ?: beingFind.timeRange.endInclusive
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

  override fun createEditableTree(): RawTreeNode {
    val root = RawTreeNode(RawTreeData.Root)
    root.addAll(codes.map { convertCodeIntoNode(it, root) })
    return root
  }

  override fun transformedByScript(ktFile: KtFile): CodeModel {
    val root = createEditableTree()
    val mod = TreeDistance.treeDistanceZhangShasha(root, RawTreeNode.buildFromPsi(ktFile))
    LOG.info("Transform cost: ${mod.sumBy { it.cost }}")
    TreeDistance.transformTree(root, mod)
    return RawTreeNode.toCodeModel(root)
  }

  fun shiftAll(delta: Long, unit: TimeUnit) {
    val ms = TimeUnit.MILLISECONDS.convert(delta, unit).floorToInt()
    val newCodes = codes.map { code ->
      code.fold<Code>({ st -> st.copy(offset = st.timeOffset + ms) }) { block, list ->
        block.copy(range = block.timeRange.shift(ms), innerBlocks = list)
      }
    }
    codes = newCodes
  }

  private fun convertCodeIntoNode(code: Code, parent: TreeNode? = null): RawTreeNode {
    val currentNode = RawTreeNode(RawTreeData.CodeData(code))
    currentNode.parent = parent
    if (code is Block) {
      for (every in code.innerBlocks) {
        currentNode.add(convertCodeIntoNode(every, currentNode))
      }
    }
    return currentNode
  }

  class MarkedTextBuilder(private val withOffsets: Boolean = true) {
    private val myBuilder = StringBuilder()
    private val myRanges = mutableListOf<IntRange>()
    private var myOffsetAccumulator = 0
    private var myIndent = 0
    private var myHasOffset = false

    fun append(str: String) {
      pushOffset()
      myBuilder.append(str)
    }

    private fun pushOffset() {
      if (!myHasOffset) {
        return
      }
      myHasOffset = false
      val text = Code.INDENTATION_UNIT * myIndent + TimeOffsetParser.createTimeOffset(myOffsetAccumulator) + "\n"
      myRanges.add(myBuilder.length until myBuilder.length + text.length)
      if (withOffsets) {
        myBuilder.append(text)
      }
      myOffsetAccumulator = 0
      myIndent = 0
    }

    fun appendOffset(indent: Int, delta: Int) {
      myHasOffset = true
      myOffsetAccumulator += delta
      myIndent = indent
      pushOffset()
    }

    fun done(): MarkedText {
      if (myOffsetAccumulator != 0) {
        pushOffset()
      }
      return MarkedText(myBuilder.toString(), myRanges)
    }
  }

  companion object {
    private val LOG = logger<CodeModel>()
  }
}

data class MarkedText(val text: String, val ranges: List<IntRange>)

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

fun codeModel(closure: CodeBlockBuilder.() -> Unit): CodeModel {
  val builder = CodeBlockBuilder()
  builder.closure()
  return CodeModel(builder.build())
}