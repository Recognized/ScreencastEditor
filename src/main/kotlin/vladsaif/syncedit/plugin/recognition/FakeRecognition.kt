package vladsaif.syncedit.plugin.recognition

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFileFactory
import vladsaif.syncedit.plugin.ClosedIntRange
import vladsaif.syncedit.plugin.TranscriptData
import vladsaif.syncedit.plugin.WordData
import vladsaif.syncedit.plugin.audioview.toolbar.OpenAudioAction
import vladsaif.syncedit.plugin.lang.transcript.psi.InternalFileType

class FakeRecognition : AnAction() {
  override fun actionPerformed(e: AnActionEvent?) {
    val demo = "file://C:/Users/User/IdeaProjects/empty/demo.wav"
    val waveform = OpenAudioAction.openAudio(e!!.project!!, VirtualFileManager.getInstance().findFileByUrl(demo)!!)!!
    val data = listOf(
        WordData("one", ClosedIntRange(1000, 2000), WordData.State.PRESENTED),
        WordData("two", ClosedIntRange(2000, 3000), WordData.State.PRESENTED),
        WordData("three", ClosedIntRange(3000, 4000), WordData.State.PRESENTED),
        WordData("four", ClosedIntRange(4000, 5000), WordData.State.PRESENTED),
        WordData("five", ClosedIntRange(5000, 6000), WordData.State.PRESENTED),
        WordData("six", ClosedIntRange(6000, 7000), WordData.State.PRESENTED),
        WordData("seven", ClosedIntRange(8000, 9000), WordData.State.PRESENTED),
        WordData("eight", ClosedIntRange(9000, 9500), WordData.State.PRESENTED),
        WordData("nine", ClosedIntRange(10000, 11000), WordData.State.PRESENTED),
        WordData("ten", ClosedIntRange(11000, 12000), WordData.State.PRESENTED),
        WordData("eleven", ClosedIntRange(12000, 13000), WordData.State.PRESENTED),
        WordData("twelve", ClosedIntRange(13000, 14000), WordData.State.PRESENTED)
    )
    val transcript = TranscriptData(data)
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runWriteAction {
        val xml = PsiFileFactory.getInstance(e.project).createFileFromText(
            "demo.${InternalFileType.defaultExtension}",
            InternalFileType,
            transcript.toXml(),
            0L,
            true
        )
        waveform.multimediaModel.setAndReadXml(xml.virtualFile)
        FileEditorManager.getInstance(e.project!!).openFile(xml.virtualFile, true)
      }
    }
  }
}