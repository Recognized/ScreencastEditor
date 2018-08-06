package vladsaif.syncedit.plugin.lang.transcript.fork

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.EmptyRunnable
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.EditorNotifications
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong

class AsyncEditorLoader(
    private val myTextEditor: TranscriptTextEditorImpl,
    private val myEditorComponent: TranscriptEditorComponent,
    private val myProvider: TranscriptEditorProvider
) {
  private val myEditor: Editor = myTextEditor.editor
  private val myProject: Project = myTextEditor.myProject
  private val myDelayedActions = ArrayList<Runnable>()
  private var myDelayedState: TextEditorState? = null
  private val myLoadingFinished = CompletableFuture<Any>()

  init {
    myEditor.putUserData(ASYNC_LOADER, this)
    myEditorComponent.contentPanel.isVisible = false
  }

  fun start(): Future<*> {
    ApplicationManager.getApplication().assertIsDispatchThread()
    val continuationFuture = scheduleLoading()
    var showProgress = true
    if (worthWaiting()) {
      /*
 * Possible alternatives:
 * 1. show "Loading" from the beginning, then it'll be always noticeable at least in fade-out phase
 * 2. show a gray screen for some time and then "Loading" if it's still loading; it'll produce quick background blinking for all editors
 * 3. show non-highlighted and unfolded editor as "Loading" background and allow it to relayout at the end of loading phase
 * 4. freeze EDT a bit and hope that for small editors it'll suffice and for big ones show "Loading" after that.
 * This strategy seems to produce minimal blinking annoyance.
 */
      val continuation = resultInTimeOrNull(continuationFuture, SYNCHRONOUS_LOADING_WAITING_TIME_MS.toLong())
      if (continuation != null) {
        showProgress = false
        loadingFinished(continuation)
      }
    }
    if (showProgress) myEditorComponent.startLoading()
    return myLoadingFinished
  }

  private fun scheduleLoading(): Future<Runnable> {
    val psiDocumentManager = PsiDocumentManager.getInstance(myTextEditor.myProject)
    val document = myEditor.document
    return ourExecutor.submit<Runnable> {
      val docStamp = AtomicLong()
      val ref = Ref<Runnable>()
      try {
        while (!myEditorComponent.isDisposed) {
          ProgressIndicatorUtils.runWithWriteActionPriority({
            psiDocumentManager.commitAndRunReadAction {
              docStamp.set(document.modificationStamp)
              ref.set(if (myProject.isDisposed) EmptyRunnable.INSTANCE else myTextEditor.loadEditorInBackground())
            }
          }, ProgressIndicatorBase())
          val continuation = ref.get()
          if (continuation != null) {
            psiDocumentManager.performLaterWhenAllCommitted({
              if (docStamp.get() == document.modificationStamp)
                loadingFinished(continuation)
              else if (!myEditorComponent.isDisposed) scheduleLoading()
            }, ModalityState.any())
            return@submit continuation
          }
          ProgressIndicatorUtils.yieldToPendingWriteActions()
        }
      } finally {
        if (ref.isNull) invokeLater(Runnable { loadingFinished(null) })
      }
      null
    }
  }

  private fun worthWaiting(): Boolean {
    // cannot perform commitAndRunReadAction in parallel to EDT waiting
    return !PsiDocumentManager.getInstance(myProject).hasUncommitedDocuments() && !ApplicationManager.getApplication().isWriteAccessAllowed
  }

  private fun loadingFinished(continuation: Runnable?) {
    if (myLoadingFinished.isDone) return
    myLoadingFinished.complete(null)
    myEditor.putUserData(ASYNC_LOADER, null)

    if (myEditorComponent.isDisposed) return

    continuation?.run()

    if (myEditorComponent.isLoading) {
      myEditorComponent.stopLoading()
    }
    myEditorComponent.contentPanel.isVisible = true

    if (myDelayedState != null) {
      val state = TextEditorState()
      state.foldingState = myDelayedState!!.foldingState
      myProvider.setStateImpl(myProject, myEditor, state)
      myDelayedState = null
    }

    for (runnable in myDelayedActions) {
      myEditor.scrollingModel.disableAnimation()
      runnable.run()
    }
    myEditor.scrollingModel.enableAnimation()

    if (FileEditorManager.getInstance(myProject).selectedTextEditor === myEditor) {
      IdeFocusManager.getInstance(myProject).requestFocusInProject(myTextEditor.preferredFocusedComponent, myProject)
    }
    EditorNotifications.getInstance(myProject).updateNotifications(myTextEditor.myFile)
  }

  internal fun getEditorState(level: FileEditorStateLevel): TextEditorState {
    ApplicationManager.getApplication().assertIsDispatchThread()


    val state = myProvider.getStateImpl(myProject, myEditor, level)
    val delayedState = myDelayedState
    if (!myLoadingFinished.isDone && delayedState != null) {
      state.setDelayedFoldState(com.intellij.util.Producer { delayedState.foldingState })
    }
    return state
  }

  internal fun setEditorState(state: TextEditorState) {
    ApplicationManager.getApplication().assertIsDispatchThread()

    if (!myLoadingFinished.isDone) {
      myDelayedState = state
    }

    myProvider.setStateImpl(myProject, myEditor, state)
  }

  companion object {
    private val ourExecutor = AppExecutorUtil.createBoundedApplicationPoolExecutor("AsyncEditorLoader pool", 2)
    private val ASYNC_LOADER = Key.create<AsyncEditorLoader>("ASYNC_LOADER")
    private const val SYNCHRONOUS_LOADING_WAITING_TIME_MS = 200

    private fun invokeLater(runnable: Runnable) {
      ApplicationManager.getApplication().invokeLater(runnable, ModalityState.any())
    }

    private fun <T> resultInTimeOrNull(future: Future<T>, timeMs: Long): T? {
      try {
        return future.get(timeMs, TimeUnit.MILLISECONDS)
      } catch (ignored: InterruptedException) {
      } catch (ignored: TimeoutException) {
      } catch (e: ExecutionException) {
        throw RuntimeException(e)
      }

      return null
    }

    fun isEditorLoaded(editor: Editor): Boolean {
      return editor.getUserData(ASYNC_LOADER) == null
    }
  }
}