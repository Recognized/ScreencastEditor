package vladsaif.syncedit.plugin.audioview.waveform

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import org.picocontainer.Disposable
import vladsaif.syncedit.plugin.sound.EditionModel
import vladsaif.syncedit.plugin.sound.Player
import vladsaif.syncedit.plugin.sound.impl.DefaultEditionModel
import vladsaif.syncedit.plugin.sound.impl.PlayerImpl
import vladsaif.syncedit.plugin.util.IRange
import vladsaif.syncedit.plugin.util.LRange
import java.awt.Dimension
import java.io.IOException
import javax.swing.JScrollPane
import javax.swing.event.ChangeEvent


class WaveformController(private val waveform: JWaveform) : Disposable {
  private var myScrollPane: JScrollPane? = null
  private var myIgnoreBarChanges = false
  @Volatile
  private var myBlockScaling = false
  @Volatile
  private var myPlayState: PlayState = PlayState.Stopped
  val hasSelection: Boolean
    get() = !waveform.selectionModel.selectedRanges.isEmpty()
  val playState: PlayState
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

  fun play() {
    val state = myPlayState
    when (state) {
      is PlayState.Playing -> return
      is PlayState.Paused -> {
        myPlayState = PlayState.Playing(state.player)
        state.player.resume()
      }
      is PlayState.Stopped -> {
        if (!waveform.model.screencast.isAudioSet) {
          return
        }
        val editionModel = if (waveform.selectionModel.selectedRanges.isEmpty()) {
          waveform.model.editionModel
        } else {
          waveform.selectionModel.toEditionModel()
        }
        val player = PlayerImpl({ waveform.model.screencast.audioInputStream }, editionModel)
        player.setProcessUpdater(this::positionUpdater)
        player.setOnStopAction {
          ApplicationManager.getApplication().invokeAndWait {
            stop()
            waveform.model.playFramePosition.set(-1L)
          }
        }
        player.play {
          when (it) {
            is IOException -> {
              showNotification("I/O error occurred while playing audio. ${it.message}")
              LOG.info(it)
            }
            is SecurityException -> showNotification("Cannot access audio file due to security restrictions.")
          }
        }
        myPlayState = PlayState.Playing(player)
      }
    }
  }

  private fun SelectionModel.toEditionModel(): EditionModel {
    val editionModel = DefaultEditionModel()
    val audioModel = waveform.model.screencast.audioDataModel ?: return editionModel
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

  fun pause() {
    val state = myPlayState
    when (state) {
      is PlayState.Stopped, is PlayState.Paused -> return
      is PlayState.Playing -> {
        state.player.pause()
        myPlayState = PlayState.Paused(state.player)
      }
    }
  }

  fun stopImmediately() {
    stopBase(Player::stopImmediately)
  }

  private fun stop() {
    stopBase(Player::stop)
  }

  private fun stopBase(action: Player.() -> Unit) {
    val state = myPlayState
    val player = when (state) {
      is PlayState.Stopped -> return
      is PlayState.Playing -> state.player
      is PlayState.Paused -> state.player
    }
    myPlayState = PlayState.Stopped
    player.action()
    waveform.stateChanged(ChangeEvent(this))
    waveform.model.playFramePosition.set(-1)
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


  sealed class PlayState {
    object Stopped : PlayState()
    class Playing(val player: Player) : PlayState()
    class Paused(val player: Player) : PlayState()
  }


  companion object {
    private val LOG = logger<WaveformController>()
  }
}

fun showNotification(
    content: String,
    title: String = "Error",
    type: NotificationType = NotificationType.ERROR
) {
  Notifications.Bus.notify(Notification("Screencast Editor", title, content, type))
}