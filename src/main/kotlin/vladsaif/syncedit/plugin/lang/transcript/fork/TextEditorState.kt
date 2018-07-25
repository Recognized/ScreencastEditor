package vladsaif.syncedit.plugin.lang.transcript.fork

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.impl.text.CodeFoldingState
import com.intellij.util.Producer
import java.util.*

class TextEditorState : FileEditorState {
    var carets: Array<CaretState>? = null
    var relativeCaretPosition: Int = 0 // distance from primary caret to the top of editor's viewable area in pixels

    /**
     * State which describes how editor is folded.
     * This field can be `null`.
     */
    private var myFoldingState: CodeFoldingState? = null
    private var myDelayedFoldInfoProducer: Producer<CodeFoldingState?>? = null

    // Assuming single-thread access here.
    var foldingState: CodeFoldingState?
        get() {
            if (myFoldingState == null && myDelayedFoldInfoProducer != null) {
                myFoldingState = myDelayedFoldInfoProducer!!.produce()
                if (myFoldingState != null) {
                    myDelayedFoldInfoProducer = null
                }
            }
            return myFoldingState
        }
        set(foldingState) {
            myFoldingState = foldingState
            myDelayedFoldInfoProducer = null
        }

    /**
     * Folding state is more complex than, say, line/column number, that's why it's deserialization can be performed only when
     * necessary pre-requisites are met (e.g. corresponding [Document] is created).
     *
     *
     * However, we can't be sure that those conditions are met on IDE startup (when editor states are read). Current method allows
     * to register a closure within the current state object which returns folding info if possible.
     *
     * @param producer  delayed folding info producer
     */
    fun setDelayedFoldState(producer: Producer<CodeFoldingState?>) {
        myDelayedFoldInfoProducer = producer
    }

    override fun equals(other: Any?): Boolean {
        if (other !is TextEditorState) {
            return false
        }
        val textEditorState = other as TextEditorState?
        if (!Arrays.equals(carets, textEditorState!!.carets)) return false
        if (relativeCaretPosition != textEditorState.relativeCaretPosition) return false
        val localFoldingState = foldingState
        val theirFoldingState = textEditorState.foldingState
        return !if (localFoldingState == null) theirFoldingState != null else localFoldingState != theirFoldingState

    }

    override fun hashCode(): Int {
        var result = 0
        val carets = carets
        if (carets != null) {
            for (caretState in carets) {
                result += caretState.hashCode()
            }
        }
        return result
    }

    override fun canBeMergedWith(otherState: FileEditorState, level: FileEditorStateLevel): Boolean {
        return if (otherState !is TextEditorState) false else level == FileEditorStateLevel.NAVIGATION
                && carets != null && carets!!.size == 1
                && otherState.carets != null && otherState.carets!!.size == 1
                && Math.abs(carets!![0].LINE - otherState.carets!![0].LINE) < MIN_CHANGE_DISTANCE
    }

    override fun toString(): String {
        return Arrays.toString(carets)
    }

    class CaretState {
        var LINE: Int = 0
        var COLUMN: Int = 0
        var LEAN_FORWARD: Boolean = false
        var VISUAL_COLUMN_ADJUSTMENT: Int = 0
        var SELECTION_START_LINE: Int = 0
        var SELECTION_START_COLUMN: Int = 0
        var SELECTION_END_LINE: Int = 0
        var SELECTION_END_COLUMN: Int = 0

        override fun equals(other: Any?): Boolean {
            if (other !is CaretState) {
                return false
            }

            if (COLUMN != other.COLUMN) return false
            if (LINE != other.LINE) return false
            if (LEAN_FORWARD != other.LEAN_FORWARD) return false
            if (VISUAL_COLUMN_ADJUSTMENT != other.VISUAL_COLUMN_ADJUSTMENT) return false
            if (SELECTION_START_LINE != other.SELECTION_START_LINE) return false
            if (SELECTION_START_COLUMN != other.SELECTION_START_COLUMN) return false
            if (SELECTION_END_LINE != other.SELECTION_END_LINE) return false
            return SELECTION_END_COLUMN == other.SELECTION_END_COLUMN

        }

        override fun hashCode(): Int {
            return LINE + COLUMN
        }

        override fun toString(): String {
            return "[$LINE,$COLUMN]"
        }
    }

    companion object {
        private const val MIN_CHANGE_DISTANCE = 4
    }
}
