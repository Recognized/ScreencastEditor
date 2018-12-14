package vladsaif.syncedit.plugin.lang.script.psi

import com.github.tmatek.zhangshasha.EditableTreeNode
import com.github.tmatek.zhangshasha.TreeNode
import com.github.tmatek.zhangshasha.TreeOperation
import io.bretty.console.tree.PrintableTreeNode
import io.bretty.console.tree.TreePrinter
import org.jetbrains.kotlin.psi.KtFile
import vladsaif.syncedit.plugin.lang.script.psi.RawTreeNode.IndexedEntry.CodeEntry
import vladsaif.syncedit.plugin.lang.script.psi.RawTreeNode.IndexedEntry.Offset
import vladsaif.syncedit.plugin.util.end
import vladsaif.syncedit.plugin.util.length

sealed class RawTreeData(val isBlock: Boolean) {
  val data
    get() = when (this) {
      is Label -> label
      is CodeData -> code.code
      is Root -> ""
    }

  class Label(val label: String, isBlock: Boolean) : RawTreeData(isBlock) {
    override fun toString(): String {
      return "Label:'$label', isBlock=$isBlock"
    }
  }

  class CodeData(val code: Code) : RawTreeData(code is Block) {
    override fun toString(): String {
      return "Code:'${code.code}', isBlock=${code is Block}"
    }
  }

  object Root : RawTreeData(true)
}

class RawTreeNode(var data: RawTreeData) : EditableTreeNode, PrintableTreeNode {
  private var myParent: TreeNode? = null
  private val myChildren: MutableList<RawTreeNode> = mutableListOf()
  var isNeedExpand = false
    private set

  fun add(node: RawTreeNode) {
    myChildren.add(node)
  }

  fun addAll(nodes: Collection<RawTreeNode>) {
    myChildren.addAll(nodes)
  }

  override fun children(): MutableList<out PrintableTreeNode> {
    return myChildren
  }

  override fun name(): String {
    return data.toString()
  }

  override fun setParent(newParent: TreeNode?) {
    myParent = newParent
  }

  override fun getParent(): TreeNode? = myParent

  override fun positionOfChild(child: TreeNode?): Int = myChildren.indexOf(child)

  override fun getChildren(): MutableList<out TreeNode> = myChildren

  override fun addChildAt(child: TreeNode, position: Int) {
    myChildren.add(position, child as RawTreeNode)
  }

  override fun renameNodeTo(other: TreeNode) {
    val current = data
    val newString = (other as RawTreeNode).data.data
    if (current is RawTreeData.CodeData) {
      val newCode = if (other.data.isBlock) {
        when (current.code) {
          is Statement -> {
            isNeedExpand = true
            Block(newString, current.code.timeOffset..current.code.timeOffset, listOf())
          }
          is Block -> Block(newString, current.code.timeRange, current.code.innerBlocks)
        }
      } else {
        when (current.code) {
          is Statement -> Statement(newString, current.code.timeOffset)
          is Block -> Statement(newString, current.code.timeRange.start)
        }
      }
      data = RawTreeData.CodeData(newCode)
    }
  }

  override fun deleteChild(child: TreeNode) {
    myChildren.remove(child)
  }

  override fun toString(): String {
    return TreePrinter.toString(this)
  }

  override fun getTransformationCost(operation: TreeOperation, other: TreeNode?): Int {
    if (data == RawTreeData.Root && other != null && (other as RawTreeNode).data == RawTreeData.Root) {
      return 0
    }
    if (data == RawTreeData.Root) {
      return 100000
    }
    return when (operation) {
      TreeOperation.OP_DELETE_NODE -> 10
      TreeOperation.OP_INSERT_NODE -> 10
      TreeOperation.OP_RENAME_NODE -> if (data.data == (other as? RawTreeNode)?.data?.data) 0 else 10
    }
  }

  override fun cloneNode(): TreeNode {
    return RawTreeNode(data)
  }

  sealed class IndexedEntry(var indexRange: IntRange) {
    class Offset(index: Int, val value: Int) : IndexedEntry(index..index) {
      override fun toString(): String {
        return "${indexRange.start}: Offset($value)"
      }
    }

    class CodeEntry(startIndex: Int, val value: String, val isBlock: Boolean, val needExpand: Boolean) :
      IndexedEntry(startIndex..startIndex) {
      override fun toString(): String {
        return if (!isBlock) "${indexRange.start}: Code('$value')"
        else "$indexRange: Code('$value')"
      }
    }
  }

  companion object {

    fun buildFromPsi(ktFile: KtFile): RawTreeNode {
      val nodes = BlockVisitor.fold<RawTreeNode>(ktFile) { element, list, isBlock ->
        if (isBlock) {
          RawTreeNode(
            RawTreeData.Label(element.text.substringBefore("{").trim { it.isWhitespace() }, true)
          ).also {
            it.myChildren.addAll(list)
            for (node in list) {
              node.myParent = it
            }
          }
        } else {
          RawTreeNode(RawTreeData.Label(element.text, false))
        }
      }
      val root = RawTreeNode(RawTreeData.Root)
      root.myChildren.addAll(nodes)
      for (node in nodes) {
        node.myParent = root
      }
      return root
    }

    fun buildPlainTree(root: RawTreeNode, oldRange: IntRange? = null): List<IndexedEntry> {
      val list = mutableListOf<IndexedEntry>()
      var index = 0
      var lastOffset = 0
      if (oldRange != null) {
        list.add(Offset(index++, oldRange.start))
        lastOffset = oldRange.start
      }
      for (child in root.myChildren) {
        val (newIndex, newLastOffset) = processNode(child, list, index, lastOffset)
        index = newIndex
        lastOffset = newLastOffset
      }
      if (oldRange != null) {
        if (lastOffset < oldRange.end) {
          list.add(Offset(index, oldRange.end))
        }
      }
      return list
    }

    fun toCodeModel(root: RawTreeNode, oldRange: IntRange? = null): CodeModel {
      val list = buildPlainTree(root, oldRange)
      val code = list.filterIsInstance(IndexedEntry.CodeEntry::class.java)
      val output = markRawTreeOutput(
        code,
        listOf(Offset(-1, 0)) + list.filterIsInstance(Offset::class.java)
      )
      val needExpand = mutableSetOf<Block>()
      val (codes, _) = fold<Code>(code, 0) { entry, inner ->
        val (k, total) = output.fraction[entry] ?: 1 to 1
        val range = output.time[entry]!!
        val correctedRange = (range.start + range.length / total * (k - 1))..range.end
        val res = if (entry.isBlock) {
          Block(entry.value, correctedRange, inner)
        } else {
          Statement(entry.value, correctedRange.start)
        }
        if (entry.needExpand && res is Block) {
          needExpand.add(res)
        }
        return@fold res
      }
      val model = CodeModel(codes)
      needExpand.firstOrNull()?.let { x ->
        val (_, right) = model.findDragBoundary(x, false) ?: return@let
        val rightCorrect = if (right < 0) x.timeRange.endInclusive + 2000 else right
        model.replace(x, Block(x.code, x.timeRange.start..rightCorrect, x.innerBlocks))
      }
      return model
    }

    private fun <T> fold(
      code: List<IndexedEntry.CodeEntry>,
      startIndex: Int,
      until: Int = Int.MAX_VALUE,
      operation: (IndexedEntry.CodeEntry, List<T>) -> T
    ): Pair<List<T>, Int> {
      val result = mutableListOf<T>()
      var index = startIndex
      while (index < code.size) {
        val x = code[index++]
        if (x.indexRange.end > until) {
          return result to index - 1
        }
        if (!x.isBlock) {
          result.add(operation(x, listOf()))
        } else {
          val (res, newIndex) = fold(code, index, x.indexRange.end, operation)
          index = newIndex
          result.add(operation(x, res))
        }
      }
      return result to index
    }

    private fun processNode(
      node: RawTreeNode,
      list: MutableList<IndexedEntry>,
      startIndex: Int,
      offset: Int
    ): Pair<Int, Int> {
      val data = node.data
      var index = startIndex
      var lastOffset = offset
      when (data) {
        is RawTreeData.CodeData -> {
          when (data.code) {
            is Block -> {
              if (lastOffset <= data.code.startTime) {
                list.add(Offset(index++, data.code.startTime))
                lastOffset = data.code.startTime
              }
              val block = CodeEntry(index++, data.data, true, node.isNeedExpand)
              list.add(block)
              for (child in node.myChildren) {
                val (newIndex, newLastOffset) = processNode(child, list, index, lastOffset)
                index = newIndex
                lastOffset = newLastOffset
              }
              block.indexRange = block.indexRange.start..index++
              if (lastOffset <= data.code.endTime) {
                list.add(Offset(index++, data.code.endTime))
                lastOffset = data.code.endTime
              }
            }
            is Statement -> {
              if (lastOffset <= data.code.timeOffset) {
                list.add(Offset(index++, data.code.timeOffset))
              }
              list.add(CodeEntry(index++, data.data, false, node.isNeedExpand))
            }
          }
        }
        is RawTreeData.Label -> {
          val element = CodeEntry(index++, data.data, data.isBlock, node.isNeedExpand)
          list.add(element)
          for (child in node.myChildren) {
            val (newIndex, newLastOffset) = processNode(child, list, index, lastOffset)
            index = newIndex
            lastOffset = newLastOffset
          }
          if (element.isBlock) {
            element.indexRange = element.indexRange.start..index++
          }
        }
      }
      return index to lastOffset
    }

    private data class Output(
      val time: Map<IndexedEntry.CodeEntry, IntRange>,
      val fraction: Map<IndexedEntry.CodeEntry, Pair<Int, Int>>
    )

    private fun markRawTreeOutput(
      expressions: List<IndexedEntry.CodeEntry>,
      offsets: List<IndexedEntry.Offset>
    ): Output {
      val intervals = offsets.sortedBy { it.indexRange.start }
      var j = 0
      val time = mutableMapOf<IndexedEntry.CodeEntry, IntRange>()
      val fraction = mutableMapOf<IndexedEntry.CodeEntry, Pair<Int, Int>>()
      val sameRangeElements = mutableMapOf<IntRange, MutableList<IndexedEntry.CodeEntry>>()
      out@ for (expr in expressions) {
        while (j < intervals.size) {
          val interval = intervals[j]
          if (interval.indexRange.end <= expr.indexRange.start) {
            while (j < intervals.size && intervals[j].indexRange.end <= expr.indexRange.start) j++
            j--
            var i = j + 1
            while (i < intervals.size) {
              if (expr.indexRange.end <= intervals[i].indexRange.start) {
                val range = intervals[j].value..intervals[i].value
                time[expr] = range
                sameRangeElements.computeIfAbsent(range) { mutableListOf() }.add(expr)
                continue@out
              }
              i++
            }
            break
          }
          j++
        }
        time[expr] = IntRange.EMPTY
      }
      for (list in sameRangeElements.values) {
        for ((index, expr) in list.withIndex()) {
          fraction[expr] = index + 1 to list.size
        }
      }
      return Output(time, fraction)
    }
  }
}