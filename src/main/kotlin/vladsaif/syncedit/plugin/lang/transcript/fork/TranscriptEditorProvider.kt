package vladsaif.syncedit.plugin.lang.transcript.fork

import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.CaretState
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.WriteExternalException
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.ui.update.UiNotifyConnector
import org.jdom.Element
import org.jetbrains.annotations.NonNls
import vladsaif.syncedit.plugin.lang.transcript.TranscriptData
import vladsaif.syncedit.plugin.lang.transcript.TranscriptModel
import vladsaif.syncedit.plugin.lang.transcript.refactoring.InplaceRenamer
import java.util.*

class TranscriptEditorProvider : FileEditorProvider {

    override fun accept(project: Project, file: VirtualFile): Boolean {
        val accepted = isTranscriptExtension(file.extension) &&
                file.isValid &&
                !file.isDirectory &&
                !file.fileType.isBinary
        val message = when {
            !file.isValid -> "File not valid"
            file.isDirectory -> "Is directory"
            file.fileType.isBinary -> "Is binary"
            else -> "OK"
        }
        LOG.info("$message : $file")
        return accepted && try {
            TranscriptData.createFrom(file.inputStream)
            LOG.info("Accepted")
            true
        } catch (ex: Throwable) {
            LOG.info("Not accepted because file is malformed")
            false
        }
    }

    private fun isTranscriptExtension(ext: String?) = ext == "transcript"

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val data = try {
            TranscriptData.createFrom(file.inputStream)
        } catch (ex: Throwable) {
            TranscriptData.EMPTY_DATA
        }
        val model = TranscriptModel(project, file.name, data, file)
        val psiFile = model.transcriptPsi!!

        return TranscriptTextEditorImpl(project, psiFile.viewProvider.virtualFile, this).apply {
            val marker = editor.document.createGuardedBlock(0, editor.document.textLength).apply {
                isGreedyToLeft = true
                isGreedyToRight = true
            }
            editor.document.putUserData(InplaceRenamer.GUARDED_BLOCKS, listOf(marker))
            with(editor.colorsScheme) {
                setColor(EditorColors.READONLY_FRAGMENT_BACKGROUND_COLOR, null)
            }
            EditorActionManager.getInstance().setReadonlyFragmentModificationHandler(editor.document) { }
        }
    }

    override fun getEditorTypeId() = TYPE_ID

    override fun getPolicy() = FileEditorPolicy.HIDE_DEFAULT_EDITOR

    override fun readState(element: Element, project: Project, file: VirtualFile): FileEditorState {
        val state = baseReadState(element) as TextEditorState

        // Foldings
        val child = element.getChild(FOLDING_ELEMENT)
        if (child != null) {
            val document = FileDocumentManager.getInstance().getCachedDocument(file)
            if (document == null) {
                val detachedStateCopy = JDOMUtil.internElement(child)
                state.setDelayedFoldState(com.intellij.util.Producer {
                    val document1 = FileDocumentManager.getInstance().getCachedDocument(file)
                    if (document1 == null) null else CodeFoldingManager.getInstance(project).readFoldingState(detachedStateCopy, document1)
                })
            } else {
                //PsiDocumentManager.getInstance(project).commitDocument(document);
                state.foldingState = CodeFoldingManager.getInstance(project).readFoldingState(child, document)
            }
        }
        return state
    }

    override fun writeState(_state: FileEditorState, project: Project, element: Element) {
        super.writeState(_state, project, element)

        val state = _state as TextEditorState

        // Foldings
        val foldingState = state.foldingState
        if (foldingState != null) {
            val e = Element(FOLDING_ELEMENT)
            try {
                CodeFoldingManager.getInstance(project).writeFoldingState(foldingState, e)
            } catch (ignored: WriteExternalException) {
            }

            if (!JDOMUtil.isEmpty(e)) {
                element.addContent(e)
            }
        }
    }

    fun getStateImpl(project: Project?, editor: Editor, level: FileEditorStateLevel): TextEditorState {
        val state = baseGetStateImpl(editor, level)
        // Save folding only on FULL level. It's very expensive to commit document on every
        // type (caused by undo).
        if (FileEditorStateLevel.FULL == level) {
            // Folding
            if (project != null && !project.isDisposed && !editor.isDisposed && project.isInitialized) {
                state.foldingState = CodeFoldingManager.getInstance(project).saveFoldingState(editor)
            } else {
                state.foldingState = null
            }
        }

        return state
    }

    fun setStateImpl(project: Project?, editor: Editor, state: TextEditorState) {
        baseSetStateImpl(editor, state)
        // Folding
        val foldState = state.foldingState
        if (project != null && foldState != null && AsyncEditorLoader.isEditorLoaded(editor)) {
            if (!PsiDocumentManager.getInstance(project).isCommitted(editor.document)) {
                PsiDocumentManager.getInstance(project).commitDocument(editor.document)
                LOG.error("File should be parsed when changing editor state, otherwise UI might be frozen for a considerable time")
            }
            editor.foldingModel.runBatchFoldingOperation { CodeFoldingManager.getInstance(project).restoreFoldingState(editor, foldState) }
        }
    }

    private fun baseReadState(element: Element): FileEditorState {
        val state = TextEditorState()

        try {
            val caretElements = element.getChildren(CARET_ELEMENT)
            if (caretElements.isEmpty()) {
                state.carets = arrayOf(readCaretInfo(element))
            } else {
                val carets = arrayOfNulls<TextEditorState.CaretState>(caretElements.size)
                for (i in caretElements.indices) {
                    carets[i] = readCaretInfo(caretElements[i])
                }
                state.carets = carets.requireNoNulls()
            }
        } catch (ignored: NumberFormatException) {
        }

        state.relativeCaretPosition = StringUtilRt.parseInt(element.getAttributeValue(RELATIVE_CARET_POSITION_ATTR), 0)

        return state
    }

    private fun readCaretInfo(element: Element): TextEditorState.CaretState {
        val caretState = TextEditorState.CaretState()
        caretState.LINE = parseWithDefault(element, LINE_ATTR)
        caretState.COLUMN = parseWithDefault(element, COLUMN_ATTR)
        caretState.LEAN_FORWARD = java.lang.Boolean.parseBoolean(element.getAttributeValue(LEAN_FORWARD_ATTR))
        caretState.SELECTION_START_LINE = parseWithDefault(element, SELECTION_START_LINE_ATTR)
        caretState.SELECTION_START_COLUMN = parseWithDefault(element, SELECTION_START_COLUMN_ATTR)
        caretState.SELECTION_END_LINE = parseWithDefault(element, SELECTION_END_LINE_ATTR)
        caretState.SELECTION_END_COLUMN = parseWithDefault(element, SELECTION_END_COLUMN_ATTR)
        return caretState
    }

    private fun parseWithDefault(element: Element, attributeName: String): Int {
        return StringUtilRt.parseInt(element.getAttributeValue(attributeName), 0)
    }

    private fun baseGetStateImpl(editor: Editor, level: FileEditorStateLevel): TextEditorState {
        val state = TextEditorState()
        val caretModel = editor.caretModel
        val caretsAndSelections = caretModel.caretsAndSelections
        val carets = arrayOfNulls<TextEditorState.CaretState>(caretsAndSelections.size)
        for (i in caretsAndSelections.indices) {
            val caretState = caretsAndSelections[i]
            val caretPosition = caretState.caretPosition
            val selectionStartPosition = caretState.selectionStart
            val selectionEndPosition = caretState.selectionEnd

            val s = TextEditorState.CaretState()
            s.LINE = getLine(caretPosition)
            s.COLUMN = getColumn(caretPosition)
            s.LEAN_FORWARD = caretPosition != null && caretPosition.leansForward
            s.VISUAL_COLUMN_ADJUSTMENT = caretState.visualColumnAdjustment
            s.SELECTION_START_LINE = getLine(selectionStartPosition)
            s.SELECTION_START_COLUMN = getColumn(selectionStartPosition)
            s.SELECTION_END_LINE = getLine(selectionEndPosition)
            s.SELECTION_END_COLUMN = getColumn(selectionEndPosition)
            carets[i] = s
        }
        state.carets = carets.requireNoNulls()
        // Saving scrolling proportion on UNDO may cause undesirable results of undo action fails to perform since
        // scrolling proportion restored slightly differs from what have been saved.
        state.relativeCaretPosition = if (level == FileEditorStateLevel.UNDO) Integer.MAX_VALUE else EditorUtil.calcRelativeCaretPosition(editor)
        return state
    }

    private fun getLine(pos: LogicalPosition?): Int {
        return pos?.line ?: 0
    }

    private fun getColumn(pos: LogicalPosition?): Int {
        return pos?.column ?: 0
    }

    private fun baseSetStateImpl(editor: Editor, state: TextEditorState) {
        val carets: Array<TextEditorState.CaretState>? = state.carets
        if (carets != null && carets.isNotEmpty()) {
            var newCarets = carets
            if (!editor.caretModel.supportsMultipleCarets()) newCarets = Array(1) { i -> carets[i] }
            val caretModel = editor.caretModel
            val states = ArrayList<CaretState>(newCarets.size)
            for (caretState in newCarets) {
                states.add(CaretState(LogicalPosition(caretState.LINE, caretState.COLUMN, caretState.LEAN_FORWARD),
                        caretState.VISUAL_COLUMN_ADJUSTMENT,
                        LogicalPosition(caretState.SELECTION_START_LINE, caretState.SELECTION_START_COLUMN),
                        LogicalPosition(caretState.SELECTION_END_LINE, caretState.SELECTION_END_COLUMN)))
            }
            caretModel.setCaretsAndSelections(states, false)
        }

        val relativeCaretPosition = state.relativeCaretPosition
        val scrollingRunnable = {
            if (!editor.isDisposed) {
                editor.scrollingModel.disableAnimation()
                if (relativeCaretPosition != Integer.MAX_VALUE) {
                    EditorUtil.setRelativeCaretPosition(editor, relativeCaretPosition)
                }
                editor.scrollingModel.scrollToCaret(ScrollType.RELATIVE)
                editor.scrollingModel.enableAnimation()
            }
        }
        if (ApplicationManager.getApplication().isUnitTestMode)
            scrollingRunnable()
        else
            UiNotifyConnector.doWhenFirstShown(editor.contentComponent, scrollingRunnable)
    }

    companion object {
        private val LOG = Logger.getInstance("#com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider")
        const val TYPE_ID = "transcript-file-editor"
        private val TEXT_EDITOR_KEY: Key<TextEditor> = Key.create("TEXT_EDITOR_KEY")

        fun putTextEditor(editor: Editor, textEditor: TextEditor) {
            editor.putUserData(TEXT_EDITOR_KEY, textEditor)
        }

        @NonNls
        private val LINE_ATTR = "line"
        @NonNls
        private val COLUMN_ATTR = "column"
        @NonNls
        private val LEAN_FORWARD_ATTR = "lean-forward"
        @NonNls
        private val SELECTION_START_LINE_ATTR = "selection-start-line"
        @NonNls
        private val SELECTION_START_COLUMN_ATTR = "selection-start-column"
        @NonNls
        private val SELECTION_END_LINE_ATTR = "selection-end-line"
        @NonNls
        private val SELECTION_END_COLUMN_ATTR = "selection-end-column"
        @NonNls
        private val RELATIVE_CARET_POSITION_ATTR = "relative-caret-position"
        @NonNls
        private val CARET_ELEMENT = "caret"
        @NonNls
        private val FOLDING_ELEMENT = "folding"
    }
}