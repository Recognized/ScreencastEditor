package vladsaif.syncedit.plugin.lang.transcript

import com.intellij.openapi.diagnostic.logger

class TranscriptModel(data: TranscriptData = TranscriptData.EMPTY_DATA) {
    private var _data = data
    private val listeners: MutableSet<Listener> = mutableSetOf()
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
        makeChange(newWords.toList())
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

    private fun makeChange(newWords: List<TranscriptData.WordData>) {
        val newData = TranscriptData(newWords)
        if (newData != _data) {
            _data = newData
            fireStateChanged()
        }
    }

    companion object {
        private val LOG = logger<TranscriptModel>()
    }
}
