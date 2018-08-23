package vladsaif.syncedit.plugin

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.nio.file.Path
import java.util.concurrent.locks.ReentrantLock
import javax.sound.sampled.*
import kotlin.concurrent.withLock


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
  private val recordLock = ReentrantLock()
  private var activeLine: TargetDataLine? = null
  private var start = 0L

  /**
   * @throws javax.sound.sampled.LineUnavailableException If data line cannot be acquired, or opened,
   * or if recording format is not supported.
   */
  fun startRecording(out: Path) {
    ApplicationManager.getApplication().assertIsDispatchThread()
    recordLock.lock()
    start = System.nanoTime()
    if (activeLine != null) return
    val info = DataLine.Info(TargetDataLine::class.java, RECORD_FORMAT)
    if (!AudioSystem.isLineSupported(info)) {
      recordLock.unlock()
      throw LineUnavailableException("Audio format ($RECORD_FORMAT) is not supported")
    }

    ApplicationManager.getApplication().executeOnPooledThread {
      try {
        activeLine = AudioSystem.getLine(info).cast()
        println("Start duration = ${(start - System.nanoTime()) / 1_000_000_000.0}")
      } catch (ex: Throwable) {
        ApplicationManager.getApplication().invokeAndWait { recordLock.unlock() }
        // TODO
        throw ex
      }
      try {
        activeLine!!.use { line ->
          ApplicationManager.getApplication().invokeAndWait { recordLock.unlock() }
          line.open(RECORD_FORMAT)
          line.start()
          AudioInputStream(line).use { stream ->
            AudioSystem.write(stream, AudioFileFormat.Type.WAVE, out.toFile())
          }
        }
      } catch (ex: Throwable) {
        // TODO
        throw ex
      }
    }
  }

  fun stopRecording() {
    ApplicationManager.getApplication().assertIsDispatchThread()
    recordLock.withLock {
      activeLine?.stop()
      activeLine?.close()
      activeLine = null
    }
  }
}