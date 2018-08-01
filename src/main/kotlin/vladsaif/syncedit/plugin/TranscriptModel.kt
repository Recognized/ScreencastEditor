package vladsaif.syncedit.plugin

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptFileType
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptPsiFile

/**
 * @constructor
 * @throws java.io.IOException If I/O error occurs while reading xml
 */
class TranscriptModel(
        val project: Project,
        val xmlFile: VirtualFile
) : Disposable {
    private val listeners: MutableSet<Listener> = mutableSetOf()
    private val transcriptFile: VirtualFile
    var data: TranscriptData = xmlFile.inputStream.use { TranscriptData.createFrom(it) }
        set(value) {
            if (value != field) {
                field = value
                fireStateChanged()
            }
            with(UndoManager.getInstance(project)) {
                if (isRedoInProgress || isUndoInProgress) {
                    return
                }
            }
            FileDocumentManager.getInstance().getDocument(xmlFile)?.let { doc ->
                ApplicationManager.getApplication().runWriteAction {
                    doc.setText(data.toXml())
                }
            }
        }
    val transcriptPsi: TranscriptPsiFile?
        get() {
            val doc = FileDocumentManager.getInstance().getDocument(transcriptFile) ?: return null
            return PsiDocumentManager.getInstance(project).getPsiFile(doc) as? TranscriptPsiFile
        }

    interface Listener {
        fun onDataChanged()
    }

    init {
        transcriptFile = PsiFileFactory.getInstance(project).createFileFromText(
                xmlFile.nameWithoutExtension,
                TranscriptFileType,
                data.text,
                0,
                true,
                false
        ).viewProvider.virtualFile
        fileModelMap[transcriptFile] = this
        fileModelMap[xmlFile] = this
    }

    fun addListener(listener: Listener) {
        listeners += listener
    }

    fun removeListener(listener: Listener) {
        listeners -= listener
    }

    private fun fireStateChanged() {
        for (x in listeners) x.onDataChanged()
    }

    fun replaceWords(replacements: List<Pair<Int, WordData>>) {
        data = data.replaceWords(replacements)
    }

    fun renameWord(index: Int, text: String) {
        data = data.renameWord(index, text)
    }

    fun concatenateWords(indexRange: ClosedIntRange) {
        data = data.concatenateWords(indexRange)
    }

    fun excludeWords(indices: IntArray) {
        data = data.excludeWords(indices)
    }

    fun excludeWord(index: Int) {
        data = data.excludeWord(index)
    }

    fun showWords(indices: IntArray) {
        data = data.showWords(indices)
    }

    fun muteWords(indices: IntArray) {
        data = data.muteWords(indices)
    }

    override fun dispose() {
        ApplicationManager.getApplication().invokeLater {
            FileDocumentManager.getInstance().getDocument(xmlFile)?.let { doc ->
                ApplicationManager.getApplication().runWriteAction {
                    doc.setText(data.toXml())
                }
            }
        }
    }

    companion object {
        private val LOG = logger<TranscriptModel>()
        val fileModelMap = mutableMapOf<VirtualFile, TranscriptModel>()
    }
}
