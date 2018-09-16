package vladsaif.syncedit.plugin.diffview

import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting
import com.intellij.codeInsight.daemon.impl.analysis.HighlightLevelUtil
import com.intellij.codeStyle.CodeStyleFacade
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
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
import com.intellij.openapi.ui.WindowWrapper
import com.intellij.openapi.ui.WindowWrapperBuilder
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addToStdlib.cast
import vladsaif.syncedit.plugin.IRange
import vladsaif.syncedit.plugin.MultimediaModel
import vladsaif.syncedit.plugin.WordData
import vladsaif.syncedit.plugin.audioview.toolbar.addAction
import vladsaif.syncedit.plugin.audioview.waveform.impl.MouseDragListener
import vladsaif.syncedit.plugin.lang.script.psi.BlockVisitor
import vladsaif.syncedit.plugin.lang.script.psi.TimeOffsetParser
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptFileType
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptLanguage
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptPsiFile
import java.awt.*
import java.awt.GridBagConstraints.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.ChangeListener
import javax.swing.event.MouseInputAdapter
import kotlin.math.max
import kotlin.math.min

object DiffDialogFactory {

  fun showWindow(model: MultimediaModel) {
    val holder = JPanel(GridBagLayout())
    holder.border = BorderFactory.createEmptyBorder()
    val (splitter, diffModel) = createSplitter(model)
    val vertical = JPanel(GridBagLayout())
    vertical.add(
        createTitle(model),
        GridBagBuilder().fill(HORIZONTAL).gridx(0).gridy(0).weightx(1.0).weighty(0.0).done()
    )
    vertical.add(
        splitter,
        GridBagBuilder().fill(BOTH).gridx(0).gridy(1).weightx(1.0).weighty(1.0).done()
    )
    holder.add(
        createToolbar(diffModel).component,
        GridBagBuilder().anchor(WEST).fill(HORIZONTAL).gridx(0).gridy(0).weighty(0.0).weightx(1.0).done()
    )
    holder.add(vertical, GridBagBuilder().fill(BOTH).gridx(0).gridy(1).weightx(1.0).weighty(1.0).done())
    val wrapper = WindowWrapperBuilder(WindowWrapper.Mode.MODAL, holder)
        .setTitle("${model.scriptFile!!.name} (${model.scriptFile})")
        .build()
    wrapper.component.addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent?) {
        e ?: return
        if (e.keyCode == KeyEvent.VK_Z && (SystemInfo.isMac && e.isMetaDown || e.isControlDown)) {
          diffModel.undo()
        }
        if (e.keyCode == KeyEvent.VK_Z && e.isShiftDown && (SystemInfo.isMac && e.isMetaDown || e.isControlDown)) {
          diffModel.redo()
        }
      }
    })
    Disposer.register(wrapper, splitter)
    Disposer.register(wrapper, diffModel)
    wrapper.show()
  }

  private fun createBoxedPanel(isVertical: Boolean = true): JPanel {
    val panel = JPanel()
    val box = BoxLayout(panel, if (isVertical) BoxLayout.Y_AXIS else BoxLayout.X_AXIS)
    panel.layout = box
    panel.border = BorderFactory.createEmptyBorder()
    return panel
  }


  private fun createTitle(model: MultimediaModel): JComponent {
    val panel = createBoxedPanel(false)
    panel.add(TitledSeparator(model.xmlFile!!.name))
    val right = TitledSeparator(model.scriptFile!!.name)
    right.componentOrientation = ComponentOrientation.RIGHT_TO_LEFT
    panel.add(right)
    panel.border = BorderFactory.createEmptyBorder(0, JBUI.scale(3), 0, JBUI.scale(3))
    return panel
  }

  private fun createToolbar(diffViewModel: DiffViewModel): ActionToolbar {
    val group = DefaultActionGroup()
    group.addAction(
        "Bind",
        "Associate selected",
        AllIcons.General.Add,
        { diffViewModel.bindSelected() },
        { !diffViewModel.selectedItems.empty && !diffViewModel.editorSelectionRange.empty })
    group.addAction(
        "Unbind",
        "Remove associations",
        AllIcons.General.Remove,
        { diffViewModel.unbindSelected() },
        { !diffViewModel.selectedItems.empty })
    group.addAction(
        "Undo",
        "Undo last action",
        AllIcons.Actions.Undo,
        { diffViewModel.undo() },
        { diffViewModel.isUndoAvailable })
    group.addAction(
        "Redo",
        "",
        AllIcons.Actions.Redo,
        { diffViewModel.redo() },
        { diffViewModel.isRedoAvailable })
    group.addAction(
        "Reset",
        "Reset all changes",
        AllIcons.Actions.Reset,
        { diffViewModel.resetChanges() },
        { true })
    return ActionManager.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, group, true)
  }


  private fun createSplitter(model: MultimediaModel): Pair<Splitter, DiffViewModel> {
    val pane = createTranscriptView(model.transcriptPsi!!)
    val editorView = createEditorPanel(model.project, model.scriptPsi!!)
    val textPanel = pane.viewport.view as TextItemPanel
    if (model.isNeedInitialBind) {
      model.createDefaultBinding()
      model.isNeedInitialBind = false
    }
    val diffModel = DiffViewModel(DiffModel(model), editorView.editor as EditorEx, textPanel.cast())
    val leftDragListener = object : MouseDragListener() {
      override fun onDrag(point: Point) {
        diffModel.selectHeightRange(IRange(min(dragStartEvent!!.y, point.y), max(dragStartEvent!!.y, point.y)))
      }
    }
    textPanel.addMouseListener(leftDragListener)
    textPanel.addMouseMotionListener(leftDragListener)
    val clickListener = object : MouseInputAdapter() {
      override fun mouseClicked(e: MouseEvent?) {
        e ?: return
        val number = textPanel.findItemNumber(e.point)
        if (number < 0 || !SwingUtilities.isLeftMouseButton(e)) {
          diffModel.selectedItems = IRange.EMPTY_RANGE
        } else {
          diffModel.selectedItems = IRange(number, number)
        }
      }
    }
    textPanel.addMouseListener(clickListener)
    textPanel.addMouseMotionListener(object : MouseInputAdapter() {
      override fun mouseExited(e: MouseEvent?) {
        diffModel.hoveredItem = -1
      }

      override fun mouseMoved(e: MouseEvent?) {
        e ?: return
        val number = textPanel.findItemNumber(e.point)
        diffModel.hoveredItem = if (number < 0) -1 else number
      }
    })
    val painter = SplitterPainter(
        diffModel,
        createTranscriptLocator(pane.viewport),
        createScriptLocator(editorView.editor)
    )
    val splitter = Splitter(
        leftComponent = pane,
        rightComponent = editorView,
        painter = painter
    )
    diffModel.addChangeListener(ChangeListener {
      pane.revalidate()
      pane.repaint()
      splitter.repaint()
    })
    editorView.editor.scrollPane.verticalScrollBar.addAdjustmentListener {
      splitter.divider.repaint()
    }
    pane.verticalScrollBar.addAdjustmentListener {
      splitter.divider.repaint()
    }
    Disposer.register(splitter, editorView)
    return splitter to diffModel
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
    var needAddSeparator = false
    for (word in words) {
      if (word.state == WordData.State.EXCLUDED) continue
      if (needAddSeparator) {
        panel.add(Box.createRigidArea(Dimension(0, JBUI.scale(1))))
      }
      panel.add(TextItem(word.filteredText))
      needAddSeparator = true
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
    val text = if (isScript) transformScript(psi as KtFile)
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
  private fun transformScript(psi: KtFile): String {
    val document = psi.viewProvider.document!!
    val linesToDelete = mutableListOf<Int>()
    BlockVisitor.visit(psi) {
      if (TimeOffsetParser.isTimeOffset(it)) {
        linesToDelete.add(document.getLineNumber(it.textOffset))
      }
    }
    return document.text.split("\n")
        .withIndex()
        .filter { (line, _) -> line !in linesToDelete }
        .joinToString(separator = "\n") { it.value }
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
      (markupModel as EditorMarkupModel).isErrorStripeVisible = false
      gutterComponentEx.setShowDefaultGutterPopup(false)
      gutterComponentEx.revalidateMarkup()
      foldingModel.isFoldingEnabled = false
      UIUtil.removeScrollBorder(component)
      HighlightLevelUtil.forceRootHighlighting(psi, FileHighlightingSetting.SKIP_HIGHLIGHTING)
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