package vladsaif.syncedit.plugin.editor.audioview.waveform

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
import vladsaif.syncedit.plugin.util.intersect
import java.io.IOException
import javax.swing.event.ChangeEvent


class WaveformController(private val view: WaveformView) : Disposable {

  @Volatile
  private var myPlayState: PlayState = PlayState.Stopped
  val hasSelection: Boolean
    get() = !view.selectionModel.selectedRanges.isEmpty()
  val playState: PlayState
    get() = myPlayState

  /**
   * Cut currently selected audio range.
   *
   * Cut fragment will be skipped when playing, but not deleted.
   */
  fun cutSelected() {
    edit(view.model.editionModel::cut)
  }

  /**
   * Mute currently selected audio range.
   *
   * Muted fragment will be silenced when playing, but not changed.
   */
  fun muteSelected() {
    edit(view.model.editionModel::mute)
  }

  fun undo() {
    edit(view.model.editionModel::undo)
  }

  private inline fun edit(consumer: (LongRange) -> Unit) {
    view.selectionModel.selectedRanges.forEach {
      val truncated = it intersect IntRange(0, view.model.maxChunks - 1)
      consumer(view.model.coordinator.pixelRangeToFrameRange(truncated))
    }
    view.stateChanged(ChangeEvent(view.model.editionModel))
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
        if (!view.model.screencast.isAudioSet) {
          return
        }
        val editionModel = if (view.selectionModel.selectedRanges.isEmpty()) {
          view.model.editionModel
        } else {
          view.selectionModel.toEditionModel()
        }
        val player = PlayerImpl({ view.model.screencast.audioInputStream }, editionModel)
        player.setProcessUpdater(this::positionUpdater)
        player.setOnStopAction {
          ApplicationManager.getApplication().invokeAndWait {
            stop()
            view.model.playFramePosition.set(-1L)
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
    val audioModel = view.model.screencast.audioDataModel ?: return editionModel
    editionModel.cut(LongRange(0, audioModel.totalFrames))
    for (selected in selectedRanges) {
      editionModel.undo(view.model.coordinator.pixelRangeToFrameRange(selected))
    }
    return editionModel
  }

  private fun positionUpdater(pos: Long) {
    view.model.playFramePosition.set(pos)
    ApplicationManager.getApplication().invokeLater {
      view.stateChanged(ChangeEvent(this))
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
    view.stateChanged(ChangeEvent(this))
    view.model.playFramePosition.set(-1)
  }

  override fun dispose() {
    stopImmediately()
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