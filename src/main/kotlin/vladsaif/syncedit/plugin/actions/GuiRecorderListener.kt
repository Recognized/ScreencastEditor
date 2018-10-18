package vladsaif.syncedit.plugin.actions

import com.intellij.openapi.diagnostic.logger
import com.intellij.testGuiFramework.recorder.GuiRecorderListener
import vladsaif.syncedit.plugin.Timer
import vladsaif.syncedit.plugin.actions.internal.RecordingManager

object GuiRecorderListener : GuiRecorderListener() {
  private val LOG = logger<GuiRecorderListener>()

  override fun beforeRecordingStart() {
    Timer.start()
    RecordingManager.startRecording()
  }

  override fun beforeRecordingPause() {
//    RecordingManager.pauseRecording()
  }

  override fun beforeRecordingFinish() {
  }

  override fun recordingStarted() {
    LOG.info("Recording started")
  }

  override fun recordingPaused() {
//    LOG.info("Recording paused")
//    Timer.pause()
    recordingFinished()
  }

  override fun recordingFinished() {
    LOG.info("Recording finished")
    Timer.stop()
    RecordingManager.stopRecording()
  }
}