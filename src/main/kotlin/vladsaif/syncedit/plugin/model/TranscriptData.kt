package vladsaif.syncedit.plugin.model

import vladsaif.syncedit.plugin.util.IRange
import vladsaif.syncedit.plugin.util.TextFormatter
import java.io.InputStream
import java.io.StringReader
import java.io.StringWriter
import javax.xml.bind.JAXB
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement
import javax.xml.bind.annotation.adapters.XmlAdapter
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter

@XmlRootElement(name = "transcript")
class TranscriptData(words: List<WordData>) {
  @field:[XmlJavaTypeAdapter(WordsAdapter::class)]
  val words = words.sorted()
  val text: String
    get() = TextFormatter.formatLines(words.map { it.filteredText }, 120, separator = '\u00A0')
        .joinToString(separator = "\n") { it }

  // JAXB needs to access default constructor via reflection and add elements
  // so we may abuse fact that ArrayList can be assigned to kotlin List
  @Suppress("unused")
  private constructor() : this(ArrayList())

  operator fun get(index: Int): WordData = words[index]

  fun replaceWords(replacements: List<Pair<Int, WordData>>): TranscriptData {
    val newWords = words.toMutableList()
    for ((index, word) in replacements) {
      newWords[index] = word
    }
    return TranscriptData(newWords.toList())
  }

  private fun replaceWord(index: Int, word: WordData): TranscriptData {
    return replaceWords(listOf(index to word))
  }

  fun renameWord(index: Int, text: String): TranscriptData {
    val newWord = words[index].copy(text = text)
    return replaceWord(index, newWord)
  }

  fun concatenateWords(indexRange: IRange): TranscriptData {
    val concat = words.subList(indexRange.start, indexRange.end + 1)
    if (concat.size < 2) return this
    val concatText = concat.joinToString(separator = " ") { it.filteredText }
    val newWord = WordData(
        concatText,
        IRange(concat.first().range.start, concat.last().range.end)
    )
    val newWords = mutableListOf<WordData>()
    newWords.addAll(words.subList(0, indexRange.start))
    newWords.add(newWord)
    newWords.addAll(words.subList(indexRange.end + 1, words.size))
    return TranscriptData(newWords)
  }

  fun excludeWords(indices: IntArray): TranscriptData {
    return replaceWords(indices.map { it to words[it].copy(state = WordData.State.EXCLUDED) })
  }

  fun excludeWord(index: Int): TranscriptData {
    return excludeWords(IntArray(1) { index })
  }

  fun showWords(indices: IntArray): TranscriptData {
    return replaceWords(indices.map { it to words[it].copy(state = WordData.State.PRESENTED) })
  }

  fun muteWords(indices: IntArray): TranscriptData {
    return replaceWords(indices.map { it to words[it].copy(state = WordData.State.MUTED) })
  }

  fun toXml(): String {
    val writer = StringWriter()
    JAXB.marshal(this, writer)
    return writer.toString()
  }

  override fun toString() = "TranscriptData($words)"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as TranscriptData
    if (words != other.words) return false
    return true
  }

  override fun hashCode(): Int {
    return words.hashCode()
  }

  private class WordsAdapter : XmlAdapter<WordsValueType, List<WordData>>() {
    override fun marshal(v: List<WordData>?): WordsValueType {
      return WordsValueType().apply { words = if (v != null) ArrayList(v) else ArrayList() }
    }

    override fun unmarshal(v: WordsValueType?): List<WordData> {
      return v?.words?.toList() ?: emptyList()
    }
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  private class WordsValueType {
    @field:[XmlElement(name = "word")]
    lateinit var words: ArrayList<WordData>
  }

  companion object {
    fun createFrom(xml: CharSequence): TranscriptData {
      return JAXB.unmarshal(StringReader(xml.toString()), TranscriptData::class.java)
    }

    fun createFrom(xml: InputStream): TranscriptData {
      return JAXB.unmarshal(xml, TranscriptData::class.java)
    }
  }
}