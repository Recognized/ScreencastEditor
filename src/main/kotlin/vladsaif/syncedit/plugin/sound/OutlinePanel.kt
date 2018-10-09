package vladsaif.syncedit.plugin.sound

import com.intellij.util.ui.JBUI
import vladsaif.syncedit.plugin.sound.SoundRecorder.State.*
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JPanel
import kotlin.math.max

class OutlinePanel : JPanel() {
  @Volatile
  private var myState = SoundRecorder.getState()

  fun updateState(state: SoundRecorder.State) {
    myState = state
    repaint()
  }

  init {
    isOpaque = false
    isVisible = true
  }

  override fun paint(g: Graphics?) {
    g ?: return
    with(g as Graphics2D) {
      val state = myState
      color = when (state) {
        IDLE -> return
        RECORDING -> Color.RED
        PAUSED -> Color.YELLOW
      }
      stroke = BasicStroke(JBUI.scale(4.0f))
      drawRect(
          0,
          0,
          max(width - 1, 0),
          max(height - 1, 0)
      )
    }
  }
}