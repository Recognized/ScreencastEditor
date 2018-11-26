package vladsaif.syncedit.plugin.editor.audioview.waveform

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import org.picocontainer.Disposable
import vladsaif.syncedit.plugin.model.ScreencastFile
import vladsaif.syncedit.plugin.sound.EditionModel
import vladsaif.syncedit.plugin.sound.Player
import vladsaif.syncedit.plugin.sound.impl.DefaultEditionModel
import vladsaif.syncedit.plugin.sound.impl.PlayerImpl
import vladsaif.syncedit.plugin.util.mapLong
import java.io.IOException
import javax.swing.Timer
import javax.swing.event.ChangeEvent


class WaveformController(private val view: WaveformView) : Disposable {

  @Volatile
  private var myPlayState: PlayState = PlayState.Stopped
  private val myScreencast: ScreencastFile = view.model.screencast
  private val myTimer = Timer(1000 / 30) {
    view.model.playFramePosition = when (val state = myPlayState) {
      is PlayState.Playing -> state.player.getFramePosition()
      PlayState.Stopped -> -1
      is PlayState.Paused -> return@Timer
    }
  }
  val hasSelection: Boolean get() = !view.selectionModel.selectedRange.isEmpty()
  val playState: PlayState get() = myPlayState

  fun cutSelected() {
    edit(EditionModel::cut)
  }

  fun muteSelected() {
    edit(EditionModel::mute)
  }

  fun unmuteSelected() {
    edit(EditionModel::unmute)
  }

  private inline fun edit(crossinline consumer: EditionModel.(LongRange) -> Unit) {
    myScreencast.performModification {
      val range = view.selectionModel.selectedRange
      editionModel.consumer(myScreencast.editionModel.overlay(myScreencast.coordinator.toFrameRange(range)))
    }
    view.stateChanged(ChangeEvent(view.model.editionModel))
  }

  fun play() {
    val state = myPlayState
    when (state) {
      is PlayState.Playing -> return
      is PlayState.Paused -> {
        myTimer.start()
        myPlayState = PlayState.Playing(state.player)
        state.player.resume()
      }
      is PlayState.Stopped -> {
        if (!view.model.screencast.isAudioSet) {
          return
        }
        val editionModel = if (view.selectionModel.selectedRange.isEmpty()) {
          view.model.editionModel
        } else {
          view.selectionModel.toEditionModel()
        }
        val player = PlayerImpl({ view.model.screencast.audioInputStream }, editionModel)
        player.setOnStopAction {
          ApplicationManager.getApplication().invokeAndWait {
            stop()
            view.model.playFramePosition = -1L
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
        myTimer.start()
        myPlayState = PlayState.Playing(player)
      }
    }
  }

  private fun SelectionModel.toEditionModel(): EditionModel {
    val editionModel = DefaultEditionModel()
    val audioModel = view.model.screencast.audioDataModel ?: return editionModel
    editionModel.cut(LongRange(0, audioModel.totalFrames))
    editionModel.unmute(selectedRange.mapLong { view.model.screencast.coordinator.toFrame(it) })
    return editionModel
  }

  fun pause() {
    myTimer.stop()
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

  fun stop() {
    stopBase(Player::stop)
  }

  private fun stopBase(action: Player.() -> Unit) {
    myTimer.stop()
    val state = myPlayState
    val player = when (state) {
      is PlayState.Stopped -> return
      is PlayState.Playing -> state.player
      is PlayState.Paused -> state.player
    }
    myPlayState = PlayState.Stopped
    player.action()
    view.stateChanged(ChangeEvent(this))
    view.model.playFramePosition = -1
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