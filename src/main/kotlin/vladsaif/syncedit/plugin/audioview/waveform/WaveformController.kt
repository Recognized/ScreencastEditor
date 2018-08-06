package vladsaif.syncedit.plugin.audioview.waveform

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import org.picocontainer.Disposable
import vladsaif.syncedit.plugin.ClosedIntRange
import vladsaif.syncedit.plugin.ClosedLongRange
import vladsaif.syncedit.plugin.audioview.waveform.Player.PlayState.*
import java.awt.Dimension
import java.io.File
import java.io.IOException
import javax.swing.JScrollPane
import javax.swing.event.ChangeEvent

class WaveformController(private val waveform: JWaveform) : Disposable {
  private var myScrollPane: JScrollPane? = null
  private var myPlayer: Player? = null
  private var myIgnoreBarChanges = false
  @Volatile
  private var myBlockScaling = false
  @Volatile
  private var myPlayState = STOP
  val hasSelection: Boolean
    get() = !waveform.selectionModel.selectedRanges.isEmpty()
  val playState: Player.PlayState
    get() = myPlayState

  fun installZoom(scrollPane: JScrollPane) {
    myScrollPane = scrollPane
    val brm = scrollPane.horizontalScrollBar.model
    scrollPane.horizontalScrollBar.addAdjustmentListener {
      if (myIgnoreBarChanges) return@addAdjustmentListener
      waveform.model.setRangeProperties(visibleChunks = brm.extent, firstVisibleChunk = brm.value, maxChunks = brm.maximum)
      waveform.model.updateData()
    }
  }

  fun zoomIn() {
    scale(2f)
  }

  fun zoomOut() {
    scale(0.5f)
  }

  /**
   * Cut currently selected audio range.
   *
   * Cut fragment will be skipped when playing, but not deleted.
   */
  fun cutSelected() {
    edit(waveform.model.editionModel::cut)
  }

  /**
   * Mute currently selected audio range.
   *
   * Muted fragment will be silenced when playing, but not changed.
   */
  fun muteSelected() {
    edit(waveform.model.editionModel::mute)
  }

  fun undo() {
    edit(waveform.model.editionModel::undo)
  }

  private inline fun edit(consumer: (ClosedLongRange) -> Unit) {
    waveform.selectionModel.selectedRanges.forEach {
      val truncated = it intersect ClosedIntRange(0, waveform.model.maxChunks - 1)
      consumer(waveform.model.chunkRangeToFrameRange(truncated))
    }
    waveform.stateChanged(ChangeEvent(waveform.model.editionModel))
  }

  /**
   * @throws java.io.IOException
   */
  fun play() {
    if (myPlayState == PLAY) return
    if (myPlayState == STOP) {
      myPlayState = PLAY
      val file = waveform.model.multimediaModel.audioFile ?: return
      val player = Player.create(File(file.path).toPath()).also { this.myPlayer = it }
      player.setProcessUpdater(this::positionUpdater)
      player.play()
      ApplicationManager.getApplication().executeOnPooledThread {
        try {
          player.applyEditions(waveform.model.editionModel)
        } catch (ex: IOException) {
          showNotification("I/O error occurred while playing audio. ${ex.message}")
        } catch (ex: SecurityException) {
          showNotification("Cannot access audio file due to security restrictions.")
        } finally {
          waveform.model.playFramePosition = -1
          stop()
        }
      }
    } else {
      myPlayState = PLAY
      myPlayer?.play()
    }
  }

  private fun positionUpdater(pos: Long) {
    waveform.model.playFramePosition = pos
    waveform.stateChanged(ChangeEvent(myPlayer))
  }

  fun pause() {
    if (myPlayState == STOP) return
    myPlayState = PAUSE
    myPlayer?.pause()
  }

  fun stop() {
    if (myPlayState == STOP) return
    myPlayState = STOP
    myPlayer?.stop()
  }

  override fun dispose() {
    stop()
  }

  private fun scale(factor: Float) {
    if (myBlockScaling) return
    myBlockScaling = true
    waveform.model.scale(factor) {
      myIgnoreBarChanges = true
      waveform.preferredSize = Dimension(waveform.model.maxChunks, waveform.height)
      val scrollPane = myScrollPane
      if (scrollPane != null) {
        val visible = scrollPane.viewport.visibleRect
        visible.x = waveform.model.firstVisibleChunk
        scrollPane.viewport.scrollRectToVisible(visible)
        scrollPane.horizontalScrollBar.value = visible.x
      }
      myIgnoreBarChanges = false
      waveform.selectionModel.resetSelection()
      waveform.model.fireStateChanged()
      revalidateRepaint()
      myBlockScaling = false
    }
  }

  private fun revalidateRepaint() {
    val scrollPane = myScrollPane ?: return
    scrollPane.viewport.view.revalidate()
    scrollPane.viewport.revalidate()
    scrollPane.revalidate()
    scrollPane.repaint()
    scrollPane.viewport.repaint()
    scrollPane.viewport.view.repaint()
  }
}

fun showNotification(
    content: String,
    title: String = "Error",
    type: NotificationType = NotificationType.ERROR
) {
  Notifications.Bus.notify(Notification("Screencast Editor", title, content, type))
}