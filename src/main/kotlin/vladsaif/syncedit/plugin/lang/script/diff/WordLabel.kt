package vladsaif.syncedit.plugin.lang.script.diff

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D

class WordLabel(text: String) : JBLabel(text) {


  override fun paintBorder(g: Graphics) {
    with(g as Graphics2D) {
      color = if (UIUtil.isUnderDarcula()) Color(220, 220, 220) else Color(70, 70, 70)
      drawRoundRect(0, 0, width, height, JBUI.scale(3), JBUI.scale(3))
    }
  }
}