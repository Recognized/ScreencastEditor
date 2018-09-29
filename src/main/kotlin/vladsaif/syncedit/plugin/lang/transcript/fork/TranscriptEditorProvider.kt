package vladsaif.syncedit.plugin.lang.transcript.fork

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.actionSystem.EditorActionManager
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import vladsaif.syncedit.plugin.ScreencastFile
import vladsaif.syncedit.plugin.TranscriptData
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptFileType
import vladsaif.syncedit.plugin.lang.transcript.refactoring.InplaceRenamer

class TranscriptEditorProvider : PsiAwareTextEditorProvider() {

  override fun accept(project: Project, file: VirtualFile): Boolean {
    val accepted = file.extension == TranscriptFileType.defaultExtension &&
        file.isValid &&
        !file.isDirectory &&
        !file.fileType.isBinary
    val message = when {
      !file.isValid -> "File not valid"
      file.isDirectory -> "Is directory"
      file.fileType.isBinary -> "Is binary"
      else -> "OK"
    }
    LOG.info("$message : $file")
    return accepted && try {
      TranscriptData.createFrom(file.inputStream)
      LOG.info("Accepted")
      true
    } catch (ex: Throwable) {
      LOG.info("Not accepted because file is malformed")
      false
    }
  }

  override fun createEditor(project: Project, file: VirtualFile): FileEditor {
    val textEditor = super.createEditor(project, file) as PsiAwareTextEditorImpl
    val editor = textEditor.editor as EditorEx
    val marker = editor.document.createGuardedBlock(0, editor.document.textLength).apply {
      isGreedyToLeft = true
      isGreedyToRight = true
    }
    editor.putUserData(ScreencastFile.KEY, file.getUserData(ScreencastFile.KEY))
    editor.document.putUserData(InplaceRenamer.GUARDED_BLOCKS, listOf(marker))
    editor.colorsScheme.setColor(EditorColors.READONLY_FRAGMENT_BACKGROUND_COLOR, null)
    EditorActionManager.getInstance().setReadonlyFragmentModificationHandler(editor.document) { }
    return textEditor
  }

  override fun getEditorTypeId() = TYPE_ID

  override fun getPolicy() = FileEditorPolicy.HIDE_DEFAULT_EDITOR

  companion object {
    private val LOG = Logger.getInstance("#com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorProvider")
    const val TYPE_ID = "transcript-file-editor"
  }
}