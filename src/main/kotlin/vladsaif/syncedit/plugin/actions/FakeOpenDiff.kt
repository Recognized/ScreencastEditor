package vladsaif.syncedit.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.idea.KotlinFileType
import vladsaif.syncedit.plugin.MultimediaModel
import vladsaif.syncedit.plugin.diffview.DiffDialogFactory

class FakeOpenDiff : AnAction() {
  private val myText = """
      timeOffset(ms = 2000L)
      println("1")
      println("2")
      typeText("text")

      timeOffset(3000L)
      mainMenu {
        button.click()
        timeOffset(4000L)
      }
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
      println()
    """.trimIndent()

  override fun actionPerformed(e: AnActionEvent?) {
    e ?: return
    val project = e.project ?: return
    FakeRecognition().actionPerformed(e)
    val script = PsiFileFactory.getInstance(project).createFileFromText(
        "script.kts",
        KotlinFileType.INSTANCE,
        myText,
        0,
        true,
        false
    )
    val demo = "file://C:/Users/User/IdeaProjects/empty/demo.wav"
    val file = VirtualFileManager.getInstance().findFileByUrl(demo)!!
    val model = MultimediaModel.get(file)!!
    model.scriptFile = script.virtualFile
    println(DiffDialogFactory.showWindow(model))
  }
}