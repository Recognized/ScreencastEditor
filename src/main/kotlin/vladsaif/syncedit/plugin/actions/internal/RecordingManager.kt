package vladsaif.syncedit.plugin.actions.internal

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeGlassPaneUtil
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.IdeGlassPaneImpl
import vladsaif.syncedit.plugin.actions.GeneratedCodeReceiver
import vladsaif.syncedit.plugin.actions.errorIO
import vladsaif.syncedit.plugin.format.ScreencastFileType
import vladsaif.syncedit.plugin.format.ScreencastZipper
import vladsaif.syncedit.plugin.sound.CountdownPanel
import vladsaif.syncedit.plugin.sound.SoundProvider
import vladsaif.syncedit.plugin.sound.SoundRecorder
import vladsaif.syncedit.plugin.sound.SoundRecorder.State.IDLE
import vladsaif.syncedit.plugin.sound.SoundRecorder.State.RECORDING
import vladsaif.syncedit.plugin.util.GridBagBuilder
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine

object RecordingManager {
  private val LOG = logger<RecordingManager>()
  private var CURRENT_GLASS_PANE: IdeGlassPaneImpl? = null
  private var CURRENT_RAW_AUDIO_PATH: Path? = null

  init {
    SoundRecorder.addListener(object : SoundRecorder.StateListener {
      override fun stateChanged(oldValue: SoundRecorder.State, newValue: SoundRecorder.State) {
        if (oldValue == IDLE && newValue == RECORDING) {
          val countDown = CountdownPanel(3)
          val pane = CURRENT_GLASS_PANE ?: return
          countDown.deactivationAction = {
            pane.remove(countDown)
            pane.revalidate()
            pane.repaint()
          }
          pane.add(
              countDown,
              GridBagBuilder()
                  .weightx(1.0)
                  .weighty(1.0)
                  .gridx(0)
                  .gridy(0)
                  .fill(GridBagConstraints.BOTH)
                  .done()
          )
          pane.revalidate()
          pane.repaint()
          countDown.countDown()
        }
      }
    })
  }

  fun startRecording() {
    ApplicationManager.getApplication().assertIsDispatchThread()
    val ideFrame = WindowManager.getInstance().getIdeFrame(null)
    val glassPane = IdeGlassPaneUtil.find(ideFrame.component) as IdeGlassPaneImpl
    CURRENT_GLASS_PANE = glassPane
    glassPane.layout = GridBagLayout()
    val rawAudioPath = Files.createTempFile("rawAudio", ".wave")
    CURRENT_RAW_AUDIO_PATH = rawAudioPath
    LOG.info("Raw audio path: $rawAudioPath")
    // Delete temp file on application exit
    Disposer.register(
        ApplicationManager.getApplication(),
        Disposable {
          Files.deleteIfExists(rawAudioPath)
        }
    )
    SoundRecorder.start { line ->
      Files.newOutputStream(rawAudioPath).buffered().use {
        writeWhileOpen(line, it)
      }
    }
  }

  @Throws(IOException::class)
  private fun writeWhileOpen(line: TargetDataLine, out: OutputStream): Long {
    var transferred: Long = 0
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var read: Int
    while (line.isOpen) {
      read = line.read(buffer, 0, DEFAULT_BUFFER_SIZE)
      if (read < 0) break
      out.write(buffer, 0, read)
      transferred += read.toLong()
    }
    return transferred
  }

  fun stopRecording() {
    ApplicationManager.getApplication().assertIsDispatchThread()
    SoundRecorder.stop()
    val rawDataPath = CURRENT_RAW_AUDIO_PATH
    CURRENT_GLASS_PANE = null
    CURRENT_RAW_AUDIO_PATH = null
    if (rawDataPath != null) {
      showSaveDialog(rawDataPath)
    }
  }

  private fun saveScreencast(
      screencast: Path,
      rawAudio: Path,
      name: String
  ) {
    val out = screencast.resolve(name.replace('.', '_') + ScreencastFileType.dotExtension)
    val task = object : Task.Backgroundable(null, "Saving screencast: $out", false) {
      override fun run(indicator: ProgressIndicator) {
        runInEdt {
          indicator.isIndeterminate = true
          if (!indicator.isRunning) indicator.start()
        }
        try {
          ScreencastZipper(out).use {
            it.addScript(GeneratedCodeReceiver.getAndFlush())
            it.useAudioOutputStream { out ->
              val length = Files.newInputStream(rawAudio).buffered().use { raw ->
                SoundProvider.countFrames(raw, SoundRecorder.RECORD_FORMAT)
              }
              Files.newInputStream(rawAudio).buffered().use { raw ->
                SoundProvider.getAudioInputStream(raw, SoundRecorder.RECORD_FORMAT, length).use { audio ->
                  // TODO: do not save in WAV, convert in something
                  AudioSystem.write(audio, AudioFileFormat.Type.WAVE, out)
                }
              }
            }
          }
        } catch (ex: Exception) {
          runInEdt {
            errorIO(null, ex.message)
          }
        } finally {
          Files.deleteIfExists(rawAudio)
          runInEdt {
            if (indicator.isRunning) indicator.stop()
          }
        }
      }
    }
    ApplicationManager.getApplication().invokeLater {
      ProgressManager.getInstance().run(task)
    }
  }

  private fun showSaveDialog(rawAudio: Path) {
    val res = Messages.showYesNoDialog(
        "Would you like to save screencast?",
        "Save screencast",
        null
    )
    if (res == Messages.YES) {
      val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
      FileChooser.chooseFile(descriptor, null, null) { chosen: VirtualFile? ->
        if (chosen == null) {
          clear(rawAudio)
          return@chooseFile
        }
        val name = Messages.showInputDialog(
            "Enter screencast name",
            "Save screencast",
            null,
            "screencast_${Date().toString().replace(' ', '_').replace(':', '_')}",
            NAME_VALIDATOR
        )
        if (name == null) {
          clear(rawAudio)
          return@chooseFile
        }
        saveScreencast(File(chosen.path).toPath(), rawAudio, name)
      }
    } else {
      clear(rawAudio)
    }
  }

  private val NAME_VALIDATOR = object : InputValidator {
    private val REGEX = "\\w+".toRegex()

    override fun checkInput(inputString: String?) = inputString != null && REGEX.matches(inputString)

    override fun canClose(inputString: String?) = checkInput(inputString)
  }

  private fun clear(rawAudio: Path) {
    // Not save, then do not store anything
    GeneratedCodeReceiver.getAndFlush()
    ApplicationManager.getApplication().executeOnPooledThread {
      Files.deleteIfExists(rawAudio)
    }
  }

  fun pauseRecording() {
    ApplicationManager.getApplication().assertIsDispatchThread()
    SoundRecorder.pause()
  }
}