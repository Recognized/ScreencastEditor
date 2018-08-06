package vladsaif.syncedit.plugin.lang.transcript.fork

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import vladsaif.syncedit.plugin.MultimediaModel

object TranscriptEditorDisposer {
  private val listenerKey = Key.create<MultimediaModel.Listener>("TranscriptDataChangeListenerKey")
  private val factoryListener = object : FileEditorManagerListener {

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
      val model = MultimediaModel.get(file) ?: return
      val listener = object : MultimediaModel.Listener {
        override fun onTranscriptDataChanged() {
          val psi = model.transcriptPsi ?: return
          PsiDocumentManager.getInstance(model.project).reparseFiles(listOf(psi.virtualFile), true)
        }
      }
      model.addTranscriptDataListener(listener)
      file.putUserData(listenerKey, listener)
    }

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
      val model = MultimediaModel.get(file) ?: return
      val listener = file.getUserData(listenerKey) ?: return
      model.removeTranscriptDataListener(listener)
    }
  }

  val modelKey = Key.create<MultimediaModel>("MultimediaKey")

  // Object will be lazily initialized in TranscriptEditorProvider at first transcript editor creation
  init {
    ApplicationManager.getApplication()
        .messageBus
        .connect()
        .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, factoryListener)
  }
}