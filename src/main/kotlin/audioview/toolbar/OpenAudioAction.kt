package vladsaif.syncedit.plugin.audioview.toolbar

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.Messages
import java.io.File
import java.io.IOException
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.UnsupportedAudioFileException

class OpenAudioAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent?) {
        e ?: return
        println()
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
        FileChooser.chooseFile(descriptor, e.project, e.project?.projectFile) {
            try {
                AudioSystem.getAudioFileFormat(File(it.path))
                AudioToolWindowManager.openAudioFile(e.project!!, File(it.path).toPath())
            } catch (ex: UnsupportedAudioFileException) {
                Messages.showErrorDialog(e.project,
                        "Audio file format is not supported. File: ${it.path}",
                        "Unsupported file format")
            } catch (ex: IOException) {
                println("I/O error occurred. ${ex.message}")
            }
        }
    }
}