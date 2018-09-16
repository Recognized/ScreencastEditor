package vladsaif.syncedit.plugin.actions.project

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import org.jetbrains.kotlin.idea.KotlinFileType
import vladsaif.syncedit.plugin.SoundProvider
import vladsaif.syncedit.plugin.actions.errorUnsupportedAudioFile
import vladsaif.syncedit.plugin.actions.tools.BindAction
import javax.sound.sampled.UnsupportedAudioFileException

class BindAudioWithScriptAction : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val audio = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE)
    try {
      audio.inputStream.use {
        SoundProvider.getAudioFileFormat(it.buffered())
      }
    } catch (ex: UnsupportedAudioFileException) {
      errorUnsupportedAudioFile(e.project!!, audio)
      return
    }
    val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor(KotlinFileType.INSTANCE)
    descriptor.title = "Choose script to associate with this audio"
    FileChooser.chooseFile(
        descriptor,
        e.project!!,
        e.project!!.projectFile
    ) { script ->
      BindAction.bind(
          e.project!!,
          audio = audio,
          script = script
      )
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.getData(CommonDataKeys.VIRTUAL_FILE) != null
        && e.project != null
  }
}