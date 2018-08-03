package vladsaif.syncedit.plugin.audioview.toolbar

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import vladsaif.syncedit.plugin.audioview.waveform.WaveformModel
import java.io.File
import java.io.IOException
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.UnsupportedAudioFileException

class OpenAudioAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent?) {
        e ?: return
        val project = e.project ?: return
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
        FileChooser.chooseFile(descriptor, project, project.projectFile) {
            openAudio(project, it)
        }
    }

    companion object {

        fun openAudio(project: Project, file: VirtualFile): WaveformModel? {
            return try {
                AudioSystem.getAudioFileFormat(File(file.path))
                AudioToolWindowManager.openAudioFile(project, file)
            } catch (ex: UnsupportedAudioFileException) {
                Messages.showErrorDialog(
                        project,
                        "Audio file format is not supported. File: $file",
                        "Unsupported file format"
                )
                null
            } catch (ex: IOException) {
                Messages.showErrorDialog(
                        project,
                        "I/O error occurred. ${ex.message}",
                        "I/O error"
                )
                null
            }
        }
    }
}