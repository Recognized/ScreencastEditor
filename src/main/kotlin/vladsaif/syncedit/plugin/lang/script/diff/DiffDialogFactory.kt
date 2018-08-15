package vladsaif.syncedit.plugin.lang.script.diff

import com.intellij.codeStyle.CodeStyleFacade
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorMarkupModel
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.Divider
import com.intellij.openapi.ui.Splitter
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import vladsaif.syncedit.plugin.MultimediaModel
import vladsaif.syncedit.plugin.lang.script.psi.TimeOffsetParser
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptFileType
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptLanguage
import java.awt.Graphics
import javax.swing.JComponent
import javax.swing.JFrame

object DiffDialogFactory {

  fun createView(model: MultimediaModel) = DialogBuilder().let {
    JFrame("Hello").apply {
      add(JBLabel("This is the standard label"))
      isVisible = true
      revalidate()
      repaint()
    }
    it.setTitle(model.scriptFile!!.name)
    it.setCenterPanel(createCenterPanel(model))
    it.showAndGet()
  }

  private fun createCenterPanel(model: MultimediaModel): JComponent {
    val leftEditor = createEditorView(model.project, model.transcriptPsi!!)
    val rightEditor = createEditorView(model.project, model.scriptPsi!!)
    return MySplitter(
        leftComponent = leftEditor,
        rightComponent = rightEditor,
        painter = createPainter()
    )
  }

  private fun createPainter(): MyPainter {
    return MyPainter()
  }

  private fun createEditorView(project: Project, psi: PsiFile): JComponent {
    val isScript = when (psi.language) {
      KotlinLanguage.INSTANCE -> true
      TranscriptLanguage -> false
      else -> throw IllegalArgumentException()
    }
    val transformed = transformFile(project, psi, isScript)
    val editor = createEditor(project, transformed.viewProvider.document!!)
    configureEditor(project, editor, transformed, isScript)
    return editor.component
  }

  private fun transformFile(project: Project, psi: PsiFile, isScript: Boolean): PsiFile {
    val text = if (isScript) transformScript(project, psi)
    else transformTranscript(project, psi)
    return PsiFileFactory.getInstance(project).createFileFromText(
        psi.name,
        psi.fileType,
        text,
        0,
        true,
        false
    )
  }

  private fun transformTranscript(project: Project, psi: PsiFile): String {
    return PsiDocumentManager.getInstance(project).getDocument(psi)!!.text
  }

  /**
   * Remove timeOffset statements from document.
   *
   * Generally, this function should use psi structure of the file
   * and delete [org.jetbrains.kotlin.psi.KtCallExpression]'s which correspond to timeOffset(Long).
   * But for now, it is an over-complicated way of reaching the goal.
   */
  private fun transformScript(project: Project, psi: PsiFile): String {
    return PsiDocumentManager.getInstance(project)
        .getDocument(psi)!!
        .text
        .splitToSequence('\n')
        .filter { !TimeOffsetParser.isTimeOffset(it) }
        .joinToString(separator = "\n")
  }


  private fun createEditor(project: Project, document: Document): EditorEx {
    val factory = EditorFactory.getInstance()
    val kind = EditorKind.DIFF
    return factory.createViewer(document, project, kind) as EditorEx
  }

  private fun configureEditor(project: Project, editor: EditorEx, psi: PsiFile, isScript: Boolean) {
    with(editor) {
      setFile(psi.virtualFile)
      highlighter = createEditorHighlighter(project, psi)
      verticalScrollbarOrientation = if (isScript) EditorEx.VERTICAL_SCROLLBAR_RIGHT
      else EditorEx.VERTICAL_SCROLLBAR_LEFT
      if (!project.isDisposed) {
        val fileType = if (isScript) KotlinFileType.INSTANCE else TranscriptFileType
        val codeStyleFacade = CodeStyleFacade.getInstance(project)
        settings.setTabSize(codeStyleFacade.getTabSize(fileType))
        settings.setUseTabCharacter(codeStyleFacade.useTabCharacter(fileType))
      }
      settings.isCaretRowShown = false
      settings.isShowIntentionBulb = false
      settings.isFoldingOutlineShown = false
      (markupModel as EditorMarkupModel).isErrorStripeVisible = true
      gutterComponentEx.setShowDefaultGutterPopup(false)
      gutterComponentEx.revalidateMarkup()
      foldingModel.isFoldingEnabled = false
      UIUtil.removeScrollBorder(component)
      if (!isScript) {
        colorsScheme.setColor(EditorColors.READONLY_FRAGMENT_BACKGROUND_COLOR, null)
      }
    }
  }

  private fun createEditorHighlighter(project: Project, psi: PsiFile): EditorHighlighter {
    val language = psi.language
    val file = psi.viewProvider.virtualFile
    val highlighterFactory = EditorHighlighterFactory.getInstance()
    val syntaxHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(language, project, file)
    return highlighterFactory.createEditorHighlighter(syntaxHighlighter, EditorColorsManager.getInstance().globalScheme)
  }

  private class MySplitter(
      private val leftComponent: JComponent,
      private val rightComponent: JComponent,
      private val painter: MyPainter
  ) : Splitter(false, 0.5f, 0.5f, 0.5f) {

    init {
      dividerWidth = 30
      firstComponent = leftComponent
      secondComponent = rightComponent
      setHonorComponentsMinimumSize(false)
    }

    override fun createDivider(): Divider {
      return object : DividerImpl() {
        override fun paint(g: Graphics) {
          super.paint(g)
          painter.paint(g, this)
        }
      }
    }
  }

  private class MyPainter {

    fun paint(graphics: Graphics, component: JComponent) {

    }
  }
}