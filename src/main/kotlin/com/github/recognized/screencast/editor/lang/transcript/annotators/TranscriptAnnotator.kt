package com.github.recognized.screencast.editor.lang.transcript.annotators

import com.github.recognized.screencast.editor.lang.transcript.psi.TranscriptWord
import com.github.recognized.screencast.editor.model.WordData.State.MUTED
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement

class TranscriptAnnotator : Annotator {

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element is TranscriptWord && element.isValid) {
      when (element.data?.state) {
        MUTED -> {
          val annotation = holder.createInfoAnnotation(element, "Word is muted")
          annotation.textAttributes = Highlighters.MUTED_WORD
        }
        else -> Unit
      }
    }
  }
}