package com.github.recognized.screencast.editor.lang.transcript.psi

import com.intellij.psi.AbstractReparseTestCase
import com.intellij.psi.util.PsiTreeUtil
import java.util.*

class TranscriptPsiTest : AbstractReparseTestCase() {

  override fun setUp() {
    super.setUp()
    setFileType(TranscriptFileType)
    prepareFile("", "")
  }

  private fun assertWordNumerationCorrect(psi: TranscriptPsiFile) {
    var j = 0
    for (child in psi.children) {
      if (child is TranscriptWord) {
        assertEquals(j++, child.number)
      }
    }
  }

  private fun ClosedRange<Int>.random() = Random().nextInt((endInclusive + 1) - start) + start

  private fun ClosedRange<Char>.random() = (start.toInt()..endInclusive.toInt()).random().toChar()

  private fun generateRandomText(numberOfWords: Int): List<String> {
    val words = mutableListOf<String>()
    for (i in 0 until numberOfWords) {
      val length = (1..30).random()
      val builder = StringBuilder()
      for (j in 0 until length) {
        builder.append(('a'..'z').random())
      }
      words += builder.toString()
    }
    return words
  }

  fun `test parsing works`() {
    val words = generateRandomText(50)
    val text = words.joinToString(separator = " ")
    insert(text)
    assert(myDummyFile is TranscriptPsiFile)
    assertFalse(PsiTreeUtil.hasErrorElements(myDummyFile))
  }

  fun `test order correct`() {
    val words = generateRandomText(50)
    val text = words.joinToString(separator = " ")
    insert(text)
    assert(myDummyFile is TranscriptPsiFile)
    assertFalse(PsiTreeUtil.hasErrorElements(myDummyFile))
    assertWordNumerationCorrect(myDummyFile as TranscriptPsiFile)
  }

  fun `test order correct after edition`() {
    val words = generateRandomText(50)
    val text = words.joinToString(separator = " ")
    insert(text)
    moveEditPointRight(-20)
    insert("brand new word")
    assertFalse(PsiTreeUtil.hasErrorElements(myDummyFile))
    assertWordNumerationCorrect(myDummyFile as TranscriptPsiFile)
  }
}