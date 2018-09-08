package vladsaif.syncedit.plugin.lang.transcript.fork

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorMarkupModel
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.text.AsyncHighlighterUpdater
import com.intellij.openapi.fileEditor.impl.text.FileDropHandler
import com.intellij.openapi.fileTypes.FileTypeEvent
import com.intellij.openapi.fileTypes.FileTypeListener
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFilePropertyEvent
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.ex.StatusBarEx
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.util.FileContentUtilCore
import com.intellij.util.ui.JBSwingUtilities
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Graphics
import java.awt.KeyboardFocusManager
import javax.swing.JComponent

class TranscriptEditorComponent(
    private val myProject: Project,
    val file: VirtualFile,
    private val myTextEditor: TranscriptTextEditorImpl
) : JBLoadingPanel(BorderLayout(), myTextEditor), DataProvider, Disposable {
  /**
   * Document to be edited
   */
  private val myDocument: Document = FileDocumentManager.getInstance().getDocument(this.file)!!

  /**
   * @return most recently used editor. This method never returns `null`.
   */
  val editor: Editor = createEditor()

  /**
   * Whether the editor's document is modified or not
   */
  private var myModified: Boolean = false
  /**
   * Whether the editor is valid or not
   */
  private var myValid: Boolean = false

  @Volatile
  var isDisposed: Boolean = false
    private set

  /**
   * @return whether the editor's document is modified or not
   */
  val isModified: Boolean
    get() {
      assertThread()
      return myModified
    }

  /**
   * Just calculates "modified" property
   */
  private val isModifiedImpl: Boolean
    get() = FileDocumentManager.getInstance().isFileModified(file)

  /**
   * Name `isValid` is in use in `java.awt.Component`
   * so we change the name of method to `isEditorValid`
   *
   * @return whether the editor is valid or not
   */
  val isEditorValid: Boolean
    get() = myValid && !editor.isDisposed

  /**
   * Just calculates
   */
  private val isEditorValidImpl: Boolean
    get() = FileDocumentManager.getInstance().getDocument(file) != null

  init {
    myDocument.addDocumentListener(MyDocumentListener(), this)
    editor.component.isFocusable = false
    add(editor.component, BorderLayout.CENTER)
    myModified = isModifiedImpl
    myValid = isEditorValidImpl
    LOG.assertTrue(myValid)
    val myVirtualFileListener = MyVirtualFileListener()
    this.file.fileSystem.addVirtualFileListener(myVirtualFileListener)
    Disposer.register(this, Disposable { this.file.fileSystem.removeVirtualFileListener(myVirtualFileListener) })
    val myConnection = myProject.messageBus.connect(this)
    myConnection.subscribe(FileTypeManager.TOPIC, MyFileTypeListener())
    myConnection.subscribe<DumbService.DumbModeListener>(DumbService.DUMB_MODE, object : DumbService.DumbModeListener {
      override fun enteredDumbMode() {
        updateHighlighters()
      }

      override fun exitDumbMode() {
        updateHighlighters()
      }
    })
  }

  /**
   * Disposes all resources allocated be the TextEditorComponent. It disposes all created
   * editors, unregisters listeners. The behaviour of the splitter after disposing is
   * unpredictable.
   */
  override fun dispose() {
    disposeEditor()
    isDisposed = true
  }

  /**
   * Should be invoked when the corresponding `TextEditorImpl`
   * is selected. Updates the status bar.
   */
  fun selectNotify() {
    updateStatusBar()
  }

  private fun createEditor(): Editor {
    val editor = EditorFactory.getInstance().createEditor(myDocument, myProject, file, false, EditorKind.MAIN_EDITOR)
    (editor.markupModel as EditorMarkupModel).isErrorStripeVisible = true
    (editor as EditorEx).gutterComponentEx.setForceShowRightFreePaintersArea(true)

    editor.setFile(file)

    editor.contextMenuGroupId = IdeActions.GROUP_EDITOR_POPUP

    (editor as EditorImpl).setDropHandler(FileDropHandler(editor))

    TranscriptEditorProvider.putTextEditor(editor, myTextEditor)
    return editor
  }

  private fun disposeEditor() {
    EditorFactory.getInstance().releaseEditor(editor)
  }

  /**
   * Updates "modified" property and fires event if necessary
   */
  fun updateModifiedProperty() {
    val oldModified = myModified
    myModified = isModifiedImpl
    myTextEditor.firePropertyChange(FileEditor.PROP_MODIFIED, oldModified, myModified)
  }

  private fun updateValidProperty() {
    val oldValid = myValid
    myValid = isEditorValidImpl
    myTextEditor.firePropertyChange(FileEditor.PROP_VALID, oldValid, myValid)
  }

  /**
   * Updates editors' highlighters. This should be done when the opened file
   * changes its file type.
   */
  private fun updateHighlighters() {
    if (!myProject.isDisposed && !editor.isDisposed) {
      AsyncHighlighterUpdater.updateHighlighters(myProject, editor, file)
    }
  }

  /**
   * Updates frame's status bar: insert/overwrite mode, caret position
   */
  private fun updateStatusBar() {
    val statusBar = WindowManager.getInstance().getStatusBar(myProject) as StatusBarEx
    statusBar.updateWidgets()
  }

  private fun validateCurrentEditor(): Editor? {
    val focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner
    if (focusOwner is JComponent) {
      if (focusOwner.getClientProperty("AuxEditorComponent") != null) return null // Hack for EditorSearchComponent
    }

    return editor
  }

  override fun getData(dataId: String): Any? {
    val e = validateCurrentEditor()
    if (e == null || e.isDisposed) return null

    // There's no FileEditorManager for default project (which is used in diff command-line application)
    if (!myProject.isDisposed && !myProject.isDefault) {
      val o = FileEditorManager.getInstance(myProject).getData(dataId, e, e.caretModel.currentCaret)
      if (o != null) return o
    }

    if (CommonDataKeys.EDITOR.`is`(dataId)) {
      return e
    }
    return if (CommonDataKeys.VIRTUAL_FILE.`is`(dataId)) {
      if (file.isValid) file else null  // fix for SCR 40329
    } else null
  }

  /**
   * Updates "modified" property
   */
  private inner class MyDocumentListener : DocumentListener {
    /**
     * We can reuse this runnable to decrease number of allocated object.
     */
    private val myUpdateRunnable: Runnable
    private var myUpdateScheduled: Boolean = false

    init {
      myUpdateRunnable = Runnable {
        myUpdateScheduled = false
        updateModifiedProperty()
      }
    }

    override fun documentChanged(e: DocumentEvent) {
      if (!myUpdateScheduled) {
        // document's timestamp is changed later on undo or PSI changes
        ApplicationManager.getApplication().invokeLater(myUpdateRunnable)
        myUpdateScheduled = true
      }
    }
  }

  /**
   * Listen changes of file types. When type of the file changes we need
   * to also change highlighter.
   */
  private inner class MyFileTypeListener : FileTypeListener {
    override fun fileTypesChanged(event: FileTypeEvent) {
      assertThread()
      // File can be invalid after file type changing. The editor should be removed
      // by the FileEditorManager if it's invalid.
      updateValidProperty()
      updateHighlighters()
    }
  }

  /**
   * Updates "valid" property and highlighters (if necessary)
   */
  private inner class MyVirtualFileListener : VirtualFileListener {
    override fun propertyChanged(e: VirtualFilePropertyEvent) {
      if (VirtualFile.PROP_NAME == e.propertyName) {
        // File can be invalidated after file changes name (extension also
        // can changes). The editor should be removed if it's invalid.
        updateValidProperty()
        if (Comparing.equal(e.file, file) && (FileContentUtilCore.FORCE_RELOAD_REQUESTOR == e.requestor || !Comparing.equal(e.oldValue, e.newValue))) {
          updateHighlighters()
        }
      }
    }

    override fun contentsChanged(event: VirtualFileEvent) {
      if (event.isFromSave) { // commit
        assertThread()
        val file = event.file
        LOG.assertTrue(file.isValid)
        if (this@TranscriptEditorComponent.file == file) {
          updateModifiedProperty()
        }
      }
    }
  }

  override fun getBackground(): Color = UIUtil.getPanelBackground()

  override fun getComponentGraphics(g: Graphics): Graphics {
    return JBSwingUtilities.runGlobalCGTransform(this, super.getComponentGraphics(g))
  }

  companion object {
    private val LOG = logger<TranscriptEditorComponent>()

    private fun assertThread() {
      ApplicationManager.getApplication().assertIsDispatchThread()
    }
  }
}
