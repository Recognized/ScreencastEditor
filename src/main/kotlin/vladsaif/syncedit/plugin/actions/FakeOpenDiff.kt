package vladsaif.syncedit.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFileFactory
import vladsaif.syncedit.plugin.MultimediaModel
import vladsaif.syncedit.plugin.lang.script.diff.DiffDialogFactory
import vladsaif.syncedit.plugin.lang.script.psi.UIScriptFileType

class FakeOpenDiff : AnAction() {
  private val myText = """
      timeOffset(ms = 2000L)
      statement()
      call()
      anotherCall()

      timeOffset(3000L)
      startBlock {
        call()
        timeOffset(4000L)
      }
    """.trimIndent()

  override fun actionPerformed(e: AnActionEvent?) {
    e ?: return
    val project = e.project ?: return
    FakeRecognition().actionPerformed(e)
    val script = PsiFileFactory.getInstance(project).createFileFromText(
        "script.guitest",
        UIScriptFileType,
        myText,
        0,
        true,
        false
    )
    val demo = "file://C:/Users/User/IdeaProjects/empty/demo.wav"
    val file = VirtualFileManager.getInstance().findFileByUrl(demo)!!
    val model = MultimediaModel.get(file)!!
    model.scriptFile = script.virtualFile
    println(DiffDialogFactory.createView(model))
  }
}