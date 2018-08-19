package vladsaif.syncedit.plugin.diffview

import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.LayoutManager
import javax.swing.JPanel

class TextItemPanel(layout: LayoutManager) : JPanel(layout) {

  constructor() : this(FlowLayout())

  override fun getPreferredSize(): Dimension {
    val height = components.sumBy {
      (it as? TextItem)?.getDesiredHeight(width) ?: it.height
    }
    return Dimension(parent.width, height)
  }

  fun getCoordinates(item: Int): Pair<Int, Int> {
    var sum = 0
    var index = 0
    var itemPointer = 0
    while (index < componentCount) {
      val component = getComponent(index)
      if (component is TextItem) itemPointer++
      index++
      if (itemPointer == item + 1) {
        return sum to (sum + component.height)
      }
      sum += component.height
    }
    throw IndexOutOfBoundsException("Requested item: $item, total components = $componentCount")
  }
}
