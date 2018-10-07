package vladsaif.syncedit.plugin.sound

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.openapi.project.Project
import java.io.IOException
import java.nio.file.Path
import javax.sound.sampled.*
import kotlin.concurrent.thread


object SoundRecorder {
  private val RECORD_FORMAT = AudioFormat(
      AudioFormat.Encoding.PCM_SIGNED,
      44100f,
      16,
      1,
      2,
      44100f,
      false
  )
  private var activeLine: TargetDataLine? = null

  /**
   * @throws javax.sound.sampled.LineUnavailableException If data line cannot be acquired, or opened,
   * or if recording format is not supported.
   */
  @Synchronized
  fun startRecording(out: Path, project: Project, listener: SoundRecorderListener) {
    ApplicationManager.getApplication().assertIsDispatchThread()
    val info = DataLine.Info(TargetDataLine::class.java, RECORD_FORMAT)
    if (!AudioSystem.isLineSupported(info)) {
      throw LineUnavailableException("Audio format ($RECORD_FORMAT) is not supported")
    }
    val line = AudioSystem.getLine(info) as TargetDataLine
    activeLine = line
    listener.beforeRecordingStart()
    val p = ProgressWindow(false, false, project)
    p.title = "Screencast Recorder"
    p.setDelayInMillis(80)
    p.start()
    p.text = "Preparing audio input device..."
    thread(start = true) {
      line.use {
        line.open(RECORD_FORMAT)
        line.start()
        p.stop()
        p.processFinish()
        listener.recordingStarted()
        try {
          AudioInputStream(line).use { stream ->
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, out.toFile())
          }
        } catch (ex: IOException) {
          listener.handleError(ex)
        } finally {
          synchronized(SoundRecorder) {
            activeLine = null
          }
        }
      }
    }
  }

  @Synchronized
  fun stopRecording() {
    ApplicationManager.getApplication().assertIsDispatchThread()
    activeLine?.stop()
    activeLine?.close()
    activeLine = null
  }
}