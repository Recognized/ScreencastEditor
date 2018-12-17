package com.github.recognized.screencast.editor.view.scriptview

import com.github.recognized.screencast.editor.lang.script.psi.TimeOffsetParser
import com.github.recognized.screencast.editor.model.Screencast
import com.intellij.application.options.CodeStyle
import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting
import com.intellij.codeInsight.daemon.impl.analysis.HighlightLevelUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorMarkupModel
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile
import java.awt.BorderLayout
import javax.swing.JPanel

class ScriptEditorFrame(val screencast: Screencast) {

  private inline val myProject get() = screencast.project
  private val myFile: VirtualFile

  init {
    val file = PsiFileFactory.getInstance(myProject).createFileFromText(
      screencast.name + ".kts",
      KotlinFileType.INSTANCE,
      screencast.codeModel.serialize(),
      0,
      true,
      false
    )
    myFile = file.virtualFile
  }

  fun show() {
    with(DialogBuilder()) {
      val doc = FileDocumentManager.getInstance().getDocument(myFile)!!
      setCenterPanel(createEditorPanel(myProject, PsiDocumentManager.getInstance(myProject).getPsiFile(doc)!!))
      addOkAction().setText("Commit")
      addCancelAction().setText("Cancel")
      setOkOperation {
        onOk(this, doc)
      }
      setCancelOperation {
        onCancel(this)
      }
      resizable(true)
      addDisposable(centerPanel as Disposable)
      title("Edit UI script")
      showModal(true)
    }
  }

  private fun onOk(builder: DialogBuilder, document: Document) {
    ProgressManager.getInstance().run(object : Task.Modal(myProject, "", false) {
      override fun run(indicator: ProgressIndicator) {
        ApplicationManager.getApplication().invokeAndWait {
          PsiDocumentManager.getInstance(myProject).commitDocument(document)
          val psi = PsiDocumentManager.getInstance(myProject).getPsiFile(document)!!
          if (PsiTreeUtil.hasErrorElements(psi)) {
            builder.setErrorText("File contains errors.")
          }
          try {
            val newCodeModel = TimeOffsetParser.parse(psi as KtFile)
            screencast.performModification {
              codeModel.codes = newCodeModel.codes
            }
            builder.dialogWrapper.close(0)
          } catch (ex: Throwable) {
            builder.setErrorText("File is incorrect. (${ex.message})")
          }
        }
      }
    })
  }

  private fun onCancel(builder: DialogBuilder) {
    builder.dialogWrapper.close(0)
  }

  companion object {

    private fun createEditorPanel(project: Project, psi: PsiFile): JPanel {
      val factory = EditorFactory.getInstance()
      val kind = EditorKind.DIFF
      val editor = factory.createEditor(psi.viewProvider.document!!, project, kind) as EditorEx
      configureEditor(project, editor, psi)
      return object : JPanel(BorderLayout()), Disposable {
        init {
          add(editor.component)
        }

        override fun dispose() {
          if (!editor.isDisposed) {
            EditorFactory.getInstance().releaseEditor(editor)
          }
        }
      }
    }

    private fun createEditorHighlighter(project: Project, psi: PsiFile): EditorHighlighter {
      val language = psi.language
      val file = psi.viewProvider.virtualFile
      val highlighterFactory = EditorHighlighterFactory.getInstance()
      val syntaxHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(language, project, file)
      return highlighterFactory.createEditorHighlighter(
        syntaxHighlighter,
        EditorColorsManager.getInstance().globalScheme
      )
    }

    private fun configureEditor(project: Project, editor: EditorEx, psi: PsiFile) {
      with(editor) {
        setFile(psi.virtualFile)
        highlighter = createEditorHighlighter(project, psi)
        verticalScrollbarOrientation = EditorEx.VERTICAL_SCROLLBAR_RIGHT
        if (!project.isDisposed) {
          settings.setTabSize(CodeStyle.getIndentOptions(psi).TAB_SIZE)
          settings.setUseTabCharacter(CodeStyle.getIndentOptions(psi).USE_TAB_CHARACTER)
        }
        settings.isCaretRowShown = false
        settings.isShowIntentionBulb = false
        settings.isFoldingOutlineShown = false
        (markupModel as EditorMarkupModel).isErrorStripeVisible = false
        gutterComponentEx.setShowDefaultGutterPopup(false)
        gutterComponentEx.revalidateMarkup()
        foldingModel.isFoldingEnabled = false
        HighlightLevelUtil.forceRootHighlighting(psi, FileHighlightingSetting.SKIP_HIGHLIGHTING)
      }
    }
  }
}