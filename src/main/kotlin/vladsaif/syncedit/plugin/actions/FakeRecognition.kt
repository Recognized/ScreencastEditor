package vladsaif.syncedit.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFileFactory
import vladsaif.syncedit.plugin.IRange
import vladsaif.syncedit.plugin.TranscriptData
import vladsaif.syncedit.plugin.WordData
import vladsaif.syncedit.plugin.lang.transcript.psi.InternalFileType

class FakeRecognition : AnAction() {
  override fun actionPerformed(e: AnActionEvent?) {
    val demo = "file://C:/Users/User/IdeaProjects/empty/demo.wav"
    val waveform = OpenAudioAction.openAudio(e!!.project!!, VirtualFileManager.getInstance().findFileByUrl(demo)!!)!!
    val data = listOf(
        WordData("one", IRange(1000, 2000), WordData.State.PRESENTED, -1),
        WordData("two", IRange(2000, 3000), WordData.State.PRESENTED, -1),
        WordData("three", IRange(3000, 4000), WordData.State.PRESENTED, -1),
        WordData("four", IRange(4000, 5000), WordData.State.PRESENTED, -1),
        WordData("five", IRange(5000, 6000), WordData.State.PRESENTED, -1),
        WordData("six", IRange(6000, 7000), WordData.State.PRESENTED, -1),
        WordData("seven", IRange(8000, 9000), WordData.State.PRESENTED, -1),
        WordData("eight", IRange(9000, 9500), WordData.State.PRESENTED, -1),
        WordData("nine", IRange(10000, 11000), WordData.State.PRESENTED, -1),
        WordData("ten", IRange(11000, 12000), WordData.State.PRESENTED, -1),
        WordData("eleven", IRange(12000, 13000), WordData.State.PRESENTED, -1),
        WordData("twelve", IRange(13000, 14000), WordData.State.PRESENTED, -1)
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