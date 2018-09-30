package vladsaif.syncedit.plugin

import com.intellij.openapi.command.undo.DocumentReference
import com.intellij.openapi.command.undo.DocumentReferenceManager
import com.intellij.openapi.command.undo.UndoableAction
import com.intellij.psi.PsiDocumentManager

class TranscriptUndoableAction(
    private val model: ScreencastFile,
    private val before: TranscriptData,
    private val after: TranscriptData
) : UndoableAction {

  private val myAffectedDocuments = mutableSetOf<DocumentReference>()

  init {
    model.transcriptPsi
        ?.let { PsiDocumentManager.getInstance(model.project).getDocument(it) }
        ?.let { addAffectedDocuments(DocumentReferenceManager.getInstance().create(it)) }
    model.scriptDocument
        ?.let { addAffectedDocuments(DocumentReferenceManager.getInstance().create(it)) }
  }

  override fun redo() {
    model.data = after
  }

  override fun undo() {
    model.data = before
  }

  fun addAffectedDocuments(ref: DocumentReference) {
    myAffectedDocuments.add(ref)
  }

  override fun isGlobal() = false

  override fun getAffectedDocuments() = myAffectedDocuments.toTypedArray()
}