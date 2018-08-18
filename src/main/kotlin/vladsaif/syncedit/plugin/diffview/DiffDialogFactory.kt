package vladsaif.syncedit.plugin.diffview

import com.intellij.codeStyle.CodeStyleFacade
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorMarkupModel
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import vladsaif.syncedit.plugin.MultimediaModel
import vladsaif.syncedit.plugin.lang.script.psi.TimeOffsetParser
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptFileType
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptLanguage
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptPsiFile
import java.awt.BorderLayout
import java.awt.ComponentOrientation
import java.awt.Dimension
import javax.swing.*

object DiffDialogFactory {

  fun createView(model: MultimediaModel) = DialogBuilder().let {
    it.setTitle(model.scriptFile!!.name)
    it.setCenterPanel(createSplitter(model))
    it.showAndGet()
  }

  private fun createSplitter(model: MultimediaModel): Splitter {
//    val leftEditor = createEditorView(model.project, model.transcriptPsi!!)
    val leftEditor = createTranscriptView(model.transcriptPsi!!)
    val rightEditor = createEditorPanel(model.project, model.scriptPsi!!)
    val ref = BindingsProvider(model.data!!.bindings)
    val painter = SplitterPainter(
        BindingsProvider(),
        createTranscriptLocator(leftEditor.viewport),
        createScriptLocator(rightEditor.editor as EditorEx)
    )
    val splitter = Splitter(
        leftComponent = leftEditor,
        rightComponent = rightEditor,
        painter = painter
    )
    rightEditor.editor.scrollPane.verticalScrollBar.addAdjustmentListener {
      splitter.divider.repaint()
    }
    leftEditor.verticalScrollBar.addAdjustmentListener {
      splitter.divider.repaint()
    }
    Disposer.register(splitter, rightEditor)
    return splitter
  }

  private fun createTranscriptLocator(viewport: JViewport): Locator {
    return object : Locator {
      override fun locate(item: Int): Pair<Int, Int> {
        val panel = viewport.view as TextItemPanel
        val offset = viewport.viewPosition.y
        val (top, bottom) = panel.getCoordinates(item)
        return (top - offset) to (bottom - offset)
      }
    }
  }

  private fun createScriptLocator(editor: EditorEx): Locator {
    return object : Locator {
      override fun locate(item: Int): Pair<Int, Int> {
        val offset = editor.scrollPane.viewport.viewPosition.y
        val y = editor.logicalPositionToXY(LogicalPosition(item, 0)).y - offset
        return y to (y + editor.lineHeight)
      }
    }
  }

  private fun createTranscriptView(psi: TranscriptPsiFile): JBScrollPane {
    val panel = TextItemPanel()
    panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
    val words = psi.model!!.data!!.words
    for ((index, word) in words.withIndex()) {
      panel.add(TextItem(word.filteredText))
      if (index != words.size - 1) {
        panel.add(Box.createRigidArea(Dimension(0, JBUI.scale(1))))
      }
    }
    val pane = JBScrollPane(panel)
    pane.componentOrientation = ComponentOrientation.RIGHT_TO_LEFT
    pane.verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_ALWAYS
    pane.border = BorderFactory.createEmptyBorder()
    return pane
  }

  private fun createEditorPanel(project: Project, psi: PsiFile): MyEditorWrapper {
    val isScript = when (psi.language) {
      KotlinLanguage.INSTANCE -> true
      TranscriptLanguage -> false
      else -> throw IllegalArgumentException()
    }
    val transformed = transformFile(project, psi, isScript)
    val factory = EditorFactory.getInstance()
    val kind = EditorKind.DIFF
    val editor = factory.createViewer(transformed.viewProvider.document!!, project, kind) as EditorEx
    configureEditor(project, editor, transformed, isScript)
    return MyEditorWrapper(editor)
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

  private class MyEditorWrapper(val editor: Editor) : JPanel(BorderLayout()), Disposable {

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