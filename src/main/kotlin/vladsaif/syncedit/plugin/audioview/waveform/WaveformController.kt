package vladsaif.syncedit.plugin.audioview.waveform

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import org.picocontainer.Disposable
import vladsaif.syncedit.plugin.IRange
import vladsaif.syncedit.plugin.LRange
import vladsaif.syncedit.plugin.audioview.waveform.Player.PlayState.*
import vladsaif.syncedit.plugin.audioview.waveform.impl.DefaultEditionModel
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
  @Volatile
  private var myUpdateTime: Long = -1
  //  private val myTimer: Timer = Timer(32) { timeTicks() }
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

  private inline fun edit(consumer: (LRange) -> Unit) {
    waveform.selectionModel.selectedRanges.forEach {
      val truncated = it intersect IRange(0, waveform.model.maxChunks - 1)
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
          if (waveform.selectionModel.selectedRanges.isEmpty()) {
            player.applyEditions(waveform.model.editionModel)
          } else {
            player.applyEditions(waveform.selectionModel.toEditionModel())
          }
        } catch (ex: IOException) {
          showNotification("I/O error occurred while playing audio. ${ex.message}")
        } catch (ex: SecurityException) {
          showNotification("Cannot access audio file due to security restrictions.")
        } finally {
          ApplicationManager.getApplication().invokeAndWait {
            stop()
            waveform.model.playFramePosition.set(-1L)
          }
        }
      }
    } else {
      myPlayState = PLAY
      myPlayer?.play()
//      myTimer.start()
    }
  }

  private fun SelectionModel.toEditionModel(): EditionModel {
    val editionModel = DefaultEditionModel()
    val audioModel = waveform.model.multimediaModel.audioDataModel ?: return editionModel
    editionModel.cut(LRange(0, audioModel.totalFrames))
    for (selected in selectedRanges) {
      editionModel.undo(waveform.model.chunkRangeToFrameRange(selected))
    }
    return editionModel
  }

  private fun positionUpdater(pos: Long) {
    waveform.model.playFramePosition.set(pos)
    ApplicationManager.getApplication().invokeLater {
      waveform.stateChanged(ChangeEvent(this))
    }
  }

  private fun timeTicks() {
    val audio = waveform.model.multimediaModel.audioDataModel ?: return
    val pos = waveform.model.playFramePosition.get()
    if (pos == -1L) return
    waveform.model.playFramePosition.compareAndSet(pos, pos + (audio.framesPerMillisecond * 32).toLong())
    waveform.stateChanged(ChangeEvent(this))
  }

  fun pause() {
    if (myPlayState == STOP) return
    myPlayState = PAUSE
    myPlayer?.pause()
  }

  fun stopImmediately() {
    stopBase(Player::stopImmediately)
  }

  private fun stop() {
    stopBase(Player::stop)
  }

  private fun stopBase(action: Player.() -> Unit) {
    if (myPlayState == STOP) return
    myPlayState = STOP
    myPlayer?.action()
    waveform.stateChanged(ChangeEvent(this))
    waveform.model.playFramePosition.set(-1)
    myPlayer = null
  }

  override fun dispose() {
    stopImmediately()
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