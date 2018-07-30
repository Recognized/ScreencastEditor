package vladsaif.syncedit.plugin

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
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
        private val xmlFile: VirtualFile
) : Disposable {
    private val listeners: MutableSet<Listener> = mutableSetOf()
    private val transcriptFile: VirtualFile
    var data: TranscriptData
    val transcriptPsi: TranscriptPsiFile?
        get() {
            val doc = FileDocumentManager.getInstance().getDocument(transcriptFile) ?: return null
            return PsiDocumentManager.getInstance(project).getPsiFile(doc) as? TranscriptPsiFile
        }

    interface Listener {
        fun onDataChanged()
    }

    init {
        data = TranscriptData.createFrom(xmlFile.inputStream)
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
        val newWords = data.words.toMutableList()
        for ((index, word) in replacements) {
            newWords[index] = word
        }
        println("old words: ${data.words}")
        makeChange(newWords.toList())
        println("new words: ${data.words}")
    }

    fun replaceWord(index: Int, word: WordData) {
        replaceWords(listOf(index to word))
    }

    fun renameWord(index: Int, text: String) {
        val newWord = data.words[index].copy(text = text)
        replaceWord(index, newWord)
    }

    fun concatenateWords(indexRange: ClosedIntRange) {
        val concat = data.words.subList(indexRange.start, indexRange.end + 1)
        if (concat.size < 2) return
        val concatText = concat.joinToString(separator = " ")
        println("\'$concatText\'")
        val newWord = WordData(concatText, ClosedIntRange(concat.first().range.start, concat.last().range.end), true)
        val newWords = mutableListOf<WordData>()
        newWords.addAll(data.words.subList(0, indexRange.start))
        newWords.add(newWord)
        newWords.addAll(data.words.subList(indexRange.end + 1, data.words.size))
        makeChange(newWords)
    }

    fun hideWords(indices: IntArray) {
        replaceWords(indices.map { it to data.words[it].copy(visible = false) })
    }

    fun hideWord(index: Int) {
        hideWords(IntArray(1) { index })
    }

    override fun dispose() {

    }

    private fun makeChange(newWords: List<WordData>) {
        val newData = TranscriptData(newWords)
        if (newData != data) {
            data = newData
            fireStateChanged()
        }
        FileDocumentManager.getInstance().getDocument(xmlFile)?.let { doc ->
            ApplicationManager.getApplication().runWriteAction {
                doc.setText(data.toXml())
            }
        }
    }

    companion object {
        private val LOG = logger<TranscriptModel>()
        val fileModelMap = mutableMapOf<VirtualFile, TranscriptModel>()
    }
}
