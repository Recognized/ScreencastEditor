package com.github.recognized.screencast.editor.lang.transcript.refactoring

import com.github.recognized.screencast.editor.lang.transcript.psi.TranscriptPsiFile
import com.github.recognized.screencast.editor.lang.transcript.psi.TranscriptWord
import com.github.recognized.screencast.editor.lang.transcript.psi.getSelectedWords
import com.github.recognized.screencast.editor.model.Screencast
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.CommandProcessor
import com.intellij.psi.PsiDocumentManager

abstract class TranscriptRefactoringAction : AnAction() {

  abstract fun doAction(model: Screencast, audio: Screencast.Audio, words: List<TranscriptWord>)

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getRequiredData(CommonDataKeys.PROJECT)
    val editor = e.getRequiredData(CommonDataKeys.EDITOR)
    val psi = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as TranscriptPsiFile
    val model = psi.model ?: return
    val audio = psi.audio ?: return
    CommandProcessor.getInstance().executeCommand(project, {
      doAction(model, audio, getSelectedWords(editor, psi))
    }, this.javaClass.simpleName, "ScreencastEditor", editor.document)
  }

  override fun update(e: AnActionEvent) {
    val context = e.dataContext
    val project = context.getData(CommonDataKeys.PROJECT)
    val editor = context.getData(CommonDataKeys.EDITOR)
    if (project == null || editor == null) {
      e.presentation.isEnabledAndVisible = false
      return
    }
    val psi = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
    e.presentation.isEnabledAndVisible = psi is TranscriptPsiFile
        && editor.caretModel.offset >= 0
  }
}