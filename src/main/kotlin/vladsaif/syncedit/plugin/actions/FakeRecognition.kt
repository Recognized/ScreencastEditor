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
        WordData("one big word that maybe result of some concatenation", IRange(1000, 2000), bindStatements = IRange(1, 3)),
        WordData("two", IRange(2000, 3000), bindStatements = IRange(1, 3)),
        WordData("three", IRange(3000, 4000), bindStatements = IRange(1, 3)),
        WordData("four", IRange(4000, 5000), bindStatements = IRange(1, 3)),
        WordData("five", IRange(5000, 6000)),
        WordData("six", IRange(6000, 7000), bindStatements = IRange(7, 10)),
        WordData("seven big word that maybe result of some concatenation", IRange(8000, 9000), bindStatements = IRange(15, 18)),
        WordData("eight", IRange(9000, 9500)),
        WordData("nine", IRange(10000, 11000), bindStatements = IRange(30, 40)),
        WordData("ten", IRange(11000, 12000)),
        WordData("eleven", IRange(12000, 13000)),
        WordData("twelve", IRange(13000, 14000))
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