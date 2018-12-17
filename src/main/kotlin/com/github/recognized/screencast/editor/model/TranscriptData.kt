package com.github.recognized.screencast.editor.model

import com.github.recognized.kotlin.ranges.extensions.intersects
import com.github.recognized.screencast.editor.util.TextFormatter
import gnu.trove.TIntHashSet
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
class TranscriptData private constructor(
  @field:[XmlJavaTypeAdapter(WordsAdapter::class)]
  val words: List<WordData> = listOf(),
  @field:[XmlJavaTypeAdapter(WordsAdapter::class)]
  val deletedWords: List<WordData> = listOf()
) {

  val text: String
    get() = TextFormatter.formatLines(words.map { it.filteredText }, 120, separator = '\u00A0')
      .joinToString(separator = "\n") { it }

  operator fun get(index: Int): WordData = words[index]

  // JAXB needs to access default constructor via reflection and add element
  // so we may abuse fact that ArrayList can be assigned to kotlin List
  @Suppress("unused")
  private constructor() : this(ArrayList(), ArrayList())

  fun replace(replacements: List<Pair<Int, WordData>>): TranscriptData {
    val newWords = words.toMutableList()
    for ((index, word) in replacements) {
      newWords[index] = word
    }
    return TranscriptData(newWords.toList(), deletedWords)
  }

  private fun replace(index: Int, word: WordData): TranscriptData {
    return replace(listOf(index to word))
  }

  fun rename(index: Int, text: String): TranscriptData {
    val newWord = words[index].copy(text = text)
    return replace(index, newWord)
  }

  fun concatenate(indexRange: IntRange): TranscriptData {
    val concat = words.subList(indexRange.start, indexRange.endInclusive + 1)
    if (concat.size < 2) return this
    val concatText = concat.joinToString(separator = " ") { it.filteredText }
    val newWord = WordData(
      concatText,
      IntRange(concat.first().range.start, concat.last().range.endInclusive)
    )
    val newWords = mutableListOf<WordData>()
    newWords.addAll(words.subList(0, indexRange.start))
    newWords.add(newWord)
    newWords.addAll(words.subList(indexRange.endInclusive + 1, words.size))
    return TranscriptData(newWords, deletedWords)
  }

  fun unmute(indices: IntArray): TranscriptData {
    return replace(indices.map { it to words[it].copy(state = WordData.State.PRESENTED) })
  }

  fun mute(indices: IntArray): TranscriptData {
    return replace(indices.map { it to words[it].copy(state = WordData.State.MUTED) })
  }

  fun delete(indices: IntArray): TranscriptData {
    val intSet = TIntHashSet()
    intSet.addAll(indices)
    val newWords = words.filterIndexed { x, _ -> x !in intSet }
    val deleted = words.filterIndexed { x, _ -> x in intSet }.toMutableList()
    deleted.addAll(deletedWords)
    return TranscriptData(newWords, deleted)
  }

  fun delete(index: Int): TranscriptData {
    return delete(intArrayOf(index))
  }

  fun toXml(): String {
    val writer = StringWriter()
    JAXB.marshal(this, writer)
    return writer.toString()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as TranscriptData

    if (words != other.words) return false
    if (deletedWords != other.deletedWords) return false

    return true
  }

  override fun hashCode(): Int {
    var result = words.hashCode()
    result = 31 * result + deletedWords.hashCode()
    return result
  }

  override fun toString(): String {
    return "TranscriptData(words=$words, deletedWords=$deletedWords)"
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

    fun create(words: List<WordData> = listOf(), deletedWords: List<WordData> = listOf()): TranscriptData {
      return TranscriptData(fixIntersections(words), deletedWords.sorted())
    }

    private fun fixIntersections(words: List<WordData>): List<WordData> {
      val sorted = words.filter { !it.range.isEmpty() }.sorted()
      if (sorted.asSequence().zipWithNext().all { (a, b) -> !a.range.intersects(b.range) }) {
        return sorted
      }
      val newWords = mutableListOf<WordData>()
      for (x in sorted) {
        if (newWords.isEmpty()) {
          newWords += x
        } else {
          val last = newWords.last()
          if (last.range.intersects(x.range)) {
            val points =
              intArrayOf(last.range.start, last.range.endInclusive, x.range.start, x.range.endInclusive).sorted()
            newWords[newWords.size - 1] = last.copy(range = points[0]..points[1])
            newWords += x.copy(range = points[2]..points[3])
          } else {
            newWords += x
          }
        }
      }
      return newWords
    }

    fun createFrom(xml: InputStream): TranscriptData {
      return JAXB.unmarshal(xml, TranscriptData::class.java)
    }

    fun createFrom(string: String): TranscriptData {
      return JAXB.unmarshal(StringReader(string), TranscriptData::class.java)
    }
  }
}