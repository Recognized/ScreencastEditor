package com.github.recognized.screencast.editor.view.audioview.waveform

import com.github.recognized.kotlin.ranges.extensions.mapLong
import com.github.recognized.screencast.editor.actions.showNotification
import com.github.recognized.screencast.editor.model.Screencast
import com.github.recognized.screencast.recorder.sound.EditionsModel
import com.github.recognized.screencast.recorder.sound.Player
import com.github.recognized.screencast.recorder.sound.impl.DefaultEditionsModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Disposer
import java.io.IOException
import javax.swing.Timer
import javax.swing.event.ChangeEvent


class WaveformController(private val view: WaveformView) : Disposable {

  private inline val myScreencast: Screencast get() = myWaveformModel.screencast
  private inline val myAudio get() = myWaveformModel.audio
  private inline val mySelectedRange get() = view.selectionModel.selectedRange
  private inline val myCoordinator get() = myWaveformModel.screencast.coordinator
  private inline val myWaveformModel get() = view.model
  @Volatile
  private var myPlayState: PlayState = PlayState.Stopped
  private val myTimer = Timer(1000 / 30) {
    myWaveformModel.playFramePosition = when (val state = myPlayState) {
      is PlayState.Playing -> state.player.getFramePosition()
      PlayState.Stopped -> -1
      is PlayState.Paused -> return@Timer
    }
  }
  val hasSelection: Boolean get() = !mySelectedRange.isEmpty()
  val playState: PlayState get() = myPlayState

  init {
    Disposer.register(view, this)
  }

  fun cutSelected() {
    edit(EditionsModel::cut)
  }

  fun muteSelected() {
    edit(EditionsModel::mute)
  }

  fun unmuteSelected() {
    edit(EditionsModel::unmute)
  }

  private inline fun edit(crossinline consumer: EditionsModel.(LongRange) -> Unit) {
    myScreencast.performModification {
      val range = mySelectedRange
      getEditable(myAudio).editionsModel.consumer(
        myAudio.editionsModel.overlay(
          myScreencast.coordinator.toFrameRange(
            range
          )
        )
      )
    }
    view.stateChanged(ChangeEvent(myAudio.editionsModel))
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
        val editionModel = if (mySelectedRange.isEmpty()) {
          myAudio.editionsModel
        } else {
          view.selectionModel.toEditionModel()
        }
        val player = Player.create(editionModel, myAudio.model.offsetFrames) {
          myAudio.audioInputStream
        }
        player.setOnStopAction {
          ApplicationManager.getApplication().invokeAndWait {
            stop()
            myWaveformModel.playFramePosition = -1L
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

  private fun SelectionModel.toEditionModel(): EditionsModel {
    val editionModel = DefaultEditionsModel()
    editionModel.cut(0..myAudio.model.totalFrames)
    editionModel.unmute(selectedRange.mapLong { myCoordinator.toFrame(it) })
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
    myWaveformModel.playFramePosition = -1
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