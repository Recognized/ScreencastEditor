package vladsaif.syncedit.plugin.diffview

import com.intellij.openapi.ui.Divider
import com.intellij.openapi.ui.Splitter
import com.intellij.util.ui.JBUI
import java.awt.Graphics
import javax.swing.JComponent

class Splitter(
    leftComponent: JComponent,
    rightComponent: JComponent,
    private val painter: SplitterPainter
) : Splitter(false, 0.5f, 0.2f, 0.8f) {

  init {
    dividerWidth = JBUI.scale(30)
    firstComponent = leftComponent
    secondComponent = rightComponent
    setHonorComponentsMinimumSize(false)
  }

  override fun createDivider(): Divider {
    return object : DividerImpl() {
      override fun paint(g: Graphics) {
        super.paint(g)
        painter.paint(g, this)
      }
    }
  }
}