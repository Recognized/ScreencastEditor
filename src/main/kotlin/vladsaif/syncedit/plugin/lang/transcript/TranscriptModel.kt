package vladsaif.syncedit.plugin.lang.transcript

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptFileType
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptPsiFile

class TranscriptModel(
        val project: Project,
        transcriptName: String,
        data: TranscriptData = TranscriptData.EMPTY_DATA,
        private val xmlFile: VirtualFile
) : Disposable {
    private var _data = data
    private val listeners: MutableSet<Listener> = mutableSetOf()
    private val transcriptFile = PsiFileFactory.getInstance(project).createFileFromText(
            transcriptName,
            TranscriptFileType,
            data.text,
            0,
            true,
            false
    ).viewProvider.virtualFile
    val transcriptPsi: TranscriptPsiFile?
    get() {
        val doc = FileDocumentManager.getInstance().getDocument(transcriptFile) ?: return null
        return PsiDocumentManager.getInstance(project).getPsiFile(doc) as? TranscriptPsiFile
    }

    init {
        fileModelMap[transcriptFile] = this
        fileModelMap[xmlFile] = this
    }

    val data: TranscriptData
        get() = _data

    fun addListener(listener: Listener) {
        listeners += listener
    }

    fun removeListener(listener: Listener) {
        listeners -= listener
    }

    private fun fireStateChanged() {
        for (x in listeners) x.onDataChanged()
    }

    interface Listener {
        fun onDataChanged()
    }

    fun replaceWords(replacements: List<Pair<Int, TranscriptData.WordData>>) {
        val newWords = data.words.toMutableList()
        for ((index, word) in replacements) {
            newWords[index] = word
        }
        println("old words: ${data.words}")
        makeChange(newWords.toList())
        println("new words: ${data.words}")
    }

    fun replaceWord(index: Int, word: TranscriptData.WordData) {
        replaceWords(listOf(index to word))
    }

    fun renameWord(index: Int, text: String) {
        val newWord = data.words[index].copy(text = text)
        replaceWord(index, newWord)
    }

    fun hideWords(indices: IntArray) {
        replaceWords(indices.map { it to data.words[it].copy(visible = false) })
    }

    fun hideWord(index: Int) {
        hideWords(IntArray(1) { index })
    }

    override fun dispose() {

    }

    private fun makeChange(newWords: List<TranscriptData.WordData>) {
        val newData = TranscriptData(newWords)
        if (newData != _data) {
            _data = newData
            fireStateChanged()
        }
    }

    companion object {
        private val LOG = logger<TranscriptModel>()
        val fileModelMap = mutableMapOf<VirtualFile, TranscriptModel>()
    }
}
