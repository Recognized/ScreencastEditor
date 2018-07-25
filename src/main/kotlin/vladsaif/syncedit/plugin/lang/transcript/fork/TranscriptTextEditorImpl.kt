package vladsaif.syncedit.plugin.lang.transcript.fork

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.impl.TextEditorBackgroundHighlighter
import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighterFactory
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiManager
import com.intellij.ui.EditorNotifications
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import java.util.concurrent.Future
import javax.swing.JComponent

class TranscriptTextEditorImpl(project: Project, file: VirtualFile, provider: TranscriptEditorProvider) : UserDataHolderBase(), TextEditor {
    private var myBackgroundHighlighter: TextEditorBackgroundHighlighter? = null
    private val myChangeSupport: PropertyChangeSupport
    private var myComponent: TranscriptEditorComponent
    private val myAsyncLoader: AsyncEditorLoader
    private val myLoadingFinished: Future<*>
    val myProject: Project = project
    val myFile: VirtualFile = file

    init {
        myChangeSupport = PropertyChangeSupport(this)
        myComponent = TranscriptEditorComponent(project, file, this)
        myAsyncLoader = AsyncEditorLoader(this, myComponent, provider)
        myLoadingFinished = myAsyncLoader.start()
        Disposer.register(this, myComponent)
    }

    private fun baseLoadEditorInBackground(): Runnable {
        val scheme = EditorColorsManager.getInstance().globalScheme
        val highlighter = EditorHighlighterFactory.getInstance().createEditorHighlighter(myFile, scheme, myProject)
        val editor = editor as EditorEx
        highlighter.setText(editor.document.immutableCharSequence)
        return Runnable { editor.highlighter = highlighter }
    }

    fun loadEditorInBackground(): Runnable {
        val baseAction = baseLoadEditorInBackground()
        val psiFile = PsiManager.getInstance(myProject).findFile(myFile)
        val document = FileDocumentManager.getInstance().getDocument(myFile)
        val foldingState = if (document != null && !myProject.isDefault)
            CodeFoldingManager.getInstance(myProject).buildInitialFoldings(document)
        else
            null
        return Runnable {
            baseAction.run()
            foldingState?.setToEditor(editor)
            if (psiFile != null && psiFile.isValid) {
                DaemonCodeAnalyzer.getInstance(myProject).restart(psiFile)
            }
            EditorNotifications.getInstance(myProject).updateNotifications(myFile)
        }
    }

    override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? {
        if (!AsyncEditorLoader.isEditorLoaded(editor)) {
            return null
        }

        if (myBackgroundHighlighter == null) {
            myBackgroundHighlighter = TextEditorBackgroundHighlighter(myProject, editor)
        }
        return myBackgroundHighlighter
    }

    override fun dispose() {}

    override fun getFile(): VirtualFile {
        return myFile
    }

    override fun getComponent(): TranscriptEditorComponent {
        return myComponent
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return getActiveEditor().contentComponent
    }

    override fun getEditor(): Editor {
        return getActiveEditor()
    }

    private fun getActiveEditor(): Editor {
        return myComponent.editor
    }

    override fun getName(): String {
        return file.name
    }

    override fun getState(level: FileEditorStateLevel): FileEditorState {
        return myAsyncLoader.getEditorState(level)
    }

    override fun setState(state: FileEditorState) {
        myAsyncLoader.setEditorState(state as TextEditorState)
    }

    override fun isModified(): Boolean {
        return myComponent.isModified
    }

    override fun isValid(): Boolean {
        return myComponent.isEditorValid
    }

    override fun selectNotify() {
        myComponent.selectNotify()
    }

    override fun deselectNotify() {}

    internal fun firePropertyChange(propertyName: String, oldValue: Any, newValue: Any) {
        myChangeSupport.firePropertyChange(propertyName, oldValue, newValue)
    }

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        myChangeSupport.addPropertyChangeListener(listener)
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        myChangeSupport.removePropertyChangeListener(listener)
    }

    override fun getCurrentLocation(): FileEditorLocation? {
        return TextEditorLocation(editor.caretModel.logicalPosition, this)
    }

    override fun getStructureViewBuilder(): StructureViewBuilder? {
        val document = myComponent.editor.document
        val file = FileDocumentManager.getInstance().getFile(document)
        return if (file == null || !file.isValid) null else StructureViewBuilder.PROVIDER.getStructureViewBuilder(file.fileType, file, myProject)
    }

    override fun canNavigateTo(navigatable: Navigatable): Boolean {
        return navigatable is OpenFileDescriptor && (navigatable.line >= 0 || navigatable.offset >= 0)
    }

    override fun navigateTo(navigatable: Navigatable) {
        (navigatable as OpenFileDescriptor).navigateIn(editor)
    }

    override fun toString(): String {
        return "Editor: " + myComponent.file
    }
}