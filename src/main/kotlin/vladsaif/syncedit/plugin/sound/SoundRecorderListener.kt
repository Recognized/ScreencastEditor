package vladsaif.syncedit.plugin.sound

import java.io.IOException

interface SoundRecorderListener {

  fun beforeRecordingStart()

  fun recordingStarted()

  fun handleError(exception: IOException)
}