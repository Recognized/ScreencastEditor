package com.github.recognized.screencast.editor.model

import com.github.recognized.kotlin.ranges.extensions.contains
import com.github.recognized.kotlin.ranges.extensions.shift
import com.github.recognized.screencast.editor.actions.errorScriptContainsErrors
import com.github.recognized.screencast.editor.actions.notifyCannotReadImportedAudio
import com.github.recognized.screencast.editor.format.IMPORTED_TRANSCRIPT_DATA_KEY
import com.github.recognized.screencast.editor.format.PLUGIN_TRANSCRIPT_DATA_KEY
import com.github.recognized.screencast.editor.format.zipLight
import com.github.recognized.screencast.editor.lang.script.psi.Code
import com.github.recognized.screencast.editor.lang.script.psi.CodeModel
import com.github.recognized.screencast.editor.lang.script.psi.CodeModelView
import com.github.recognized.screencast.editor.lang.script.psi.TimeOffsetParser
import com.github.recognized.screencast.editor.lang.transcript.fork.TranscriptFactoryListener
import com.github.recognized.screencast.editor.lang.transcript.psi.TranscriptFileType
import com.github.recognized.screencast.editor.lang.transcript.psi.TranscriptPsiFile
import com.github.recognized.screencast.editor.model.WordData.State.MUTED
import com.github.recognized.screencast.editor.model.WordData.State.PRESENTED
import com.github.recognized.screencast.editor.util.ExEDT
import com.github.recognized.screencast.editor.view.Coordinator
import com.github.recognized.screencast.editor.view.audioview.waveform.AudioModel
import com.github.recognized.screencast.editor.view.audioview.waveform.ShiftableAudioModel
import com.github.recognized.screencast.editor.view.audioview.waveform.impl.SimpleAudioModel
import com.github.recognized.screencast.recorder.format.ScreencastFileType
import com.github.recognized.screencast.recorder.format.ScreencastZip
import com.github.recognized.screencast.recorder.format.ScreencastZipSettings
import com.github.recognized.screencast.recorder.format.ScreencastZipSettings.Companion.IMPORTED_AUDIO_OFFSET_KEY
import com.github.recognized.screencast.recorder.format.ScreencastZipSettings.Companion.IMPORTED_EDITIONS_VIEW_KEY
import com.github.recognized.screencast.recorder.format.ScreencastZipSettings.Companion.PLUGIN_EDITIONS_VIEW_KEY
import com.github.recognized.screencast.recorder.format.ScreencastZipSettings.Companion.SCRIPT_KEY
import com.github.recognized.screencast.recorder.format.ScreencastZipper
import com.github.recognized.screencast.recorder.sound.EditionsModel
import com.github.recognized.screencast.recorder.sound.EditionsModel.EditionType.*
import com.github.recognized.screencast.recorder.sound.EditionsView
import com.github.recognized.screencast.recorder.sound.impl.DefaultEditionsModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.undo.DocumentReference
import com.intellij.openapi.command.undo.DocumentReferenceManager
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.command.undo.UndoableAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.io.exists
import com.intellij.util.io.isFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.psi.KtFile
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class Screencast(
  val project: Project,
  val file: Path
) : Disposable {

  private val myAudioEditionsListeners = ContainerUtil.newConcurrentMap<Audio, MutableSet<() -> Unit>>()
  private val myAudioTranscriptListeners = ContainerUtil.newConcurrentMap<Audio, MutableSet<() -> Unit>>()
  private val myAudioListeners = ContainerUtil.newConcurrentSet<() -> Unit>()
  private val myCodeListeners = ContainerUtil.newConcurrentSet<() -> Unit>()
  private val myCodeModel = CodeModel(listOf())
  private var myIsInsideModification: Boolean = false
  private var myIsUndoRedoAction: Boolean = false
  private var myForcedTextUpdate: Boolean = false
  private val myUndoStack: Deque<ScreencastUndoableAction> = ArrayDeque<ScreencastUndoableAction>()
  private val myRedoStack: Deque<ScreencastUndoableAction> = ArrayDeque<ScreencastUndoableAction>()
  private var myEditablePluginAudio: EditableAudio? = null
    private set(value) {
      updateAudio(field, value)
      field = value
    }
  private var myEditableImportedAudio: EditableAudio? = null
    private set(value) {
      updateAudio(field, value)
      field = value
    }
  private inline val myAudios get() = listOf(myEditableImportedAudio, myEditablePluginAudio)
  private val myZip = ScreencastZip(file)
  private var myScriptFile: VirtualFile? = null
  inline val name: String get() = file.fileName.toString().substringBefore(ScreencastFileType.dotExtension)
  inline val scriptViewDoc: Document? get() = scriptViewPsi?.viewProvider?.document
  inline val scriptDocument: Document? get() = scriptPsi?.viewProvider?.document
  var importedAudioPath: Path? = null
    private set
  val codeModel: CodeModelView = myCodeModel
  val pluginAudio: Audio? get() = myEditablePluginAudio
  val importedAudio: Audio? get() = myEditableImportedAudio
  val coordinator: Coordinator = Coordinator()
  var scriptViewFile: VirtualFile? = null
    private set
  val scriptViewPsi: KtFile? get() = getPsi(project, scriptViewFile)
  val scriptPsi: KtFile? get() = getPsi(project, myScriptFile)
  val files: List<VirtualFile>
    get() = listOfNotNull(myScriptFile, scriptViewFile) + myAudios.mapNotNull { it?.transcriptFile }

  init {
    if (!file.exists()) {
      throw IOException("File ($file) does not exist.")
    }
    if (!file.isFile() || !file.toString().endsWith(ScreencastFileType.dotExtension)) {
      throw IOException("Supplied file ($file) is not screencast.")
    }
    EditorFactory.getInstance().addEditorFactoryListener(TranscriptFactoryListener(), this)
  }

  fun isUndoAvailable(): Boolean = !myUndoStack.isEmpty()

  fun isRedoAvailable(): Boolean = !myRedoStack.isEmpty()

  fun undo() {
    undoOrRedo(true)
  }

  fun redo() {
    undoOrRedo(false)
  }

  private fun undoOrRedo(isUndo: Boolean) {
    val action = (if (isUndo) myUndoStack.pollLast() else myRedoStack.pollLast())
      ?: throw IllegalStateException("Undo/Redo is not available")
    myIsUndoRedoAction = true
    if (isUndo) {
      action.undo()
      myRedoStack.addLast(action)
    } else {
      action.redo()
      myUndoStack.addLast(action)
    }
    myIsUndoRedoAction = false
  }

  private fun getAudioState(): List<AudioState> {
    return listOf(
      myEditablePluginAudio?.getState() ?: AudioState.NoState(true),
      myEditableImportedAudio?.getState() ?: AudioState.NoState(false)
    )
  }

  fun performModification(action: ModificationScope.() -> Unit) {
    ApplicationManager.getApplication().assertIsDispatchThread()
    if (myIsInsideModification) {
      LOG.info("modification: Accidentally inside modification")
      ModificationScope().action()
      return
    }
    myIsInsideModification = true
    val audioBefore = getAudioState()
    val codesBefore = codeModel.codes
    val importedPathBefore = importedAudioPath
    val scope = ModificationScope().apply(action)
    val isCodeUpdatedByText = scope.isCodeUpdatedByText
    val importedPathAfter = importedAudioPath
    val audioAfter = getAudioState()
    val changes = audioBefore.zip(audioAfter)
    for ((before, after) in changes) {
      if (before is AudioState.ExistentState && after is AudioState.ExistentState) {
        if (before.data != null && before.data == after.data && before.editionsView != after.editionsView) {
          after.data = synchronizeWithEditionModel(after.editionsView, before.data!!)
          val origin = if (after.isPlugin) myEditablePluginAudio else myEditableImportedAudio
          origin?.data = after.data
        }
      }
    }
    val codesAfter = codeModel.codes
    if (codesBefore == codesAfter &&
      changes.all { (a, b) -> a == b } && !myIsUndoRedoAction
    ) {
      LOG.info("modification: Nothing changed...")
      myIsInsideModification = false
      return
    }
    val undoableAction = ScreencastUndoableAction(audioBefore.zip(audioAfter))
    undoableAction.importedPathUpdate = importedPathBefore to importedPathAfter
    if (codesBefore != codesAfter || isCodeUpdatedByText) {
      LOG.info("modification: Codes changed")
      undoableAction.codeUpdate = codesBefore to codesAfter
      myCodeListeners.forEach { it() }
    }
    if (!isCodeUpdatedByText) {
      myForcedTextUpdate = true
      runWriteAction {
        scriptViewDoc?.let {
          it.setText(codeModel.createTextWithoutOffsets().text)
          LOG.info("modification: Trying to rewrite script")
          UndoManager.getInstance(project)
            .nonundoableActionPerformed(DocumentReferenceManager.getInstance().create(it), false)
        }
      }
      myForcedTextUpdate = false
    } else {
      scriptViewDoc?.let {
        UndoManager.getInstance(project)
          .nonundoableActionPerformed(DocumentReferenceManager.getInstance().create(it), false)
      }
    }
    for ((before, after) in changes) {
      if (before is AudioState.ExistentState && after is AudioState.ExistentState) {
        if (before.data != after.data) {
          myAudioTranscriptListeners[after.audio]?.forEach { it.invoke() }
        }
        if (before.editionsView != after.editionsView) {
          myAudioEditionsListeners[after.audio]?.forEach { it.invoke() }
        }
        if (before.audio != after.audio) {
          myAudioListeners.forEach { it.invoke() }
        }
      }
      if (before is AudioState.NoState && after is AudioState.ExistentState
        || before is AudioState.ExistentState && after is AudioState.NoState
      ) {
        LOG.info("modification: audio changed")
        myAudioListeners.forEach { it.invoke() }
      }
    }
    if (!myIsUndoRedoAction) {
      myRedoStack.clear()
      myUndoStack.addLast(undoableAction)
      if (myUndoStack.size > MAX_UNDO_ACTIONS) {
        myUndoStack.removeFirst()
      }
    }
    LOG.info("Exiting modification")
    myIsInsideModification = false
  }

  private fun initializeScript(script: String) {
    val tempFile = createVirtualFile("$name.kts", script, KotlinFileType.INSTANCE)
    tempFile.putUserData(SCREENCAST_KEY, this@Screencast)
    PsiDocumentManager.getInstance(project).commitDocument(getPsi<KtFile>(project, tempFile)!!.viewProvider.document!!)
    if (!PsiTreeUtil.hasErrorElements(getPsi<KtFile>(project, tempFile)!!)) {
      myCodeModel.codes = TimeOffsetParser.parse(getPsi(project, tempFile)!!).codes
    } else {
      errorScriptContainsErrors(this@Screencast)
      // TODO
    }
    myScriptFile = createVirtualFile(
      "$name.kts",
      codeModel.serialize(),
      KotlinFileType.INSTANCE
    ).also { it.putUserData(SCREENCAST_KEY, this@Screencast) }
    PsiDocumentManager.getInstance(project).commitDocument(scriptDocument!!)
    val markedText = myCodeModel.createTextWithoutOffsets()
    scriptViewFile = createVirtualFile(
      "$name.kts",
      markedText.text,
      KotlinFileType.INSTANCE
    )
    scriptViewDoc!!.addDocumentListener(ChangesReproducer())
  }

  private fun initializeTranscript(transcriptData: TranscriptData, audio: EditableAudio) {
    audio.data = synchronizeWithEditionModel(audio.editionsModel, transcriptData)
  }

  suspend fun initialize() {
    val settings = myZip.readSettings()
    withContext(Dispatchers.Default) {
      if (myZip.hasPluginAudio) {
        myEditablePluginAudio = EditableAudio(true, SimpleAudioModel { myZip.audioInputStream }).apply {
          model.offsetFrames = settings[ScreencastZipSettings.PLUGIN_AUDIO_OFFSET_KEY] ?: 0L
        }
      }

      fun createEditableAudio(source: () -> InputStream): EditableAudio? {
        return try {
          EditableAudio(false, SimpleAudioModel(source)).apply {
            model.offsetFrames = settings[IMPORTED_AUDIO_OFFSET_KEY] ?: 0L
          }.also {
            importedAudioPath = null
          }
        } catch (ex: Throwable) {
          LOG.info(ex)
          null
        }
      }

      if (myZip.hasImportedAudio) {
        myEditableImportedAudio = createEditableAudio { myZip.importedAudioInputStream }
        if (importedAudio == null) {
          withContext(ExEDT) {
            notifyCannotReadImportedAudio(project)
          }
        }
      }
    }
    withContext(ExEDT) {
      pluginAudio?.let {
        coordinator.framesPerSecond = (it.model.framesPerMillisecond * 1000).toLong()
      } ?: importedAudio?.let {
        coordinator.framesPerSecond = (it.model.framesPerMillisecond * 1000).toLong()
      }
      settings[PLUGIN_EDITIONS_VIEW_KEY]?.let {
        myEditablePluginAudio?.editionsModel?.load(it)
      }
      settings[IMPORTED_EDITIONS_VIEW_KEY]?.let {
        myEditableImportedAudio?.editionsModel?.load(it)
      }
      initializeScript(settings[SCRIPT_KEY] ?: "")
      settings[PLUGIN_TRANSCRIPT_DATA_KEY]?.let { data ->
        myEditablePluginAudio?.let { initializeTranscript(data, it) }
      }
      settings[IMPORTED_TRANSCRIPT_DATA_KEY]?.let { data ->
        myEditableImportedAudio?.let { initializeTranscript(data, it) }
      }
      myAudioTranscriptListeners.forEach { (_, b) -> b.forEach { it.invoke() } }
    }
  }

  fun getLightSaveFunction(): (Path) -> Unit {
    return { out ->
      val tempFile = Files.createTempFile("screencast", "." + ScreencastFileType.defaultExtension)
      ScreencastZipper.zipLight(this, tempFile)
      Files.move(tempFile, out, StandardCopyOption.REPLACE_EXISTING)
    }
  }

  fun addTranscriptListener(audio: Audio, listener: () -> Unit) {
    myAudioTranscriptListeners.computeIfAbsent(audio) {
      ContainerUtil.newConcurrentSet<() -> Unit>()
    }.add(listener)
  }

  fun removeTranscriptListener(audio: Audio, listener: () -> Unit) {
    myAudioTranscriptListeners[audio]?.remove(listener)
  }

  fun addCodeListener(listener: () -> Unit) {
    myCodeListeners += listener
  }

  fun removeCodeListener(listener: () -> Unit) {
    myCodeListeners -= listener
  }

  fun addEditionListener(audio: Audio, listener: () -> Unit) {
    myAudioEditionsListeners.computeIfAbsent(audio) {
      ContainerUtil.newConcurrentSet<() -> Unit>()
    }.add(listener)
  }

  fun removeEditionListener(audio: Audio, listener: () -> Unit) {
    myAudioEditionsListeners[audio]?.remove(listener)
  }

  fun addAudioListener(listener: () -> Unit) {
    myAudioListeners += listener
  }

  fun removeAudioListener(listener: () -> Unit) {
    myAudioListeners -= listener
  }

//  private fun updateRangeHighlighters() {

//        )
//            HighlighterTargetArea.EXACT_RANGE
//            editor.colorsScheme.getAttributes(EditorColors.DELETED_TEXT_ATTRIBUTES),
//            10000,
//            marker.endOffset,
//            marker.startOffset,
//        (editor as EditorEx).markupModel.addRangeHighlighter(
//      for ((_, marker) in textMapping) {
//    for (editor in EditorFactory.getInstance().getEditors(document)) {
//    val document = scriptDocument ?: return
//  }

  private fun synchronizeWithEditionModel(
    editionsView: EditionsView,
    data: TranscriptData
  ): TranscriptData {
    val editions = editionsView.editionsModel.map { coordinator.toMillisecondsRange(it.first) to it.second }
    val newWords = data.words.toMutableSet()
    val deletedWords = data.deletedWords.toMutableSet()
    val processList = { list: List<WordData>, edition: Pair<IntRange, EditionsModel.EditionType> ->
      var wasInRange = false
      for (word in list) {
        if (word.range in edition.first) {
          wasInRange = true
          when (edition.second) {
            CUT -> {
              newWords -= word
              deletedWords += word
            }
            MUTE -> {
              newWords -= word
              deletedWords -= word
              newWords += if (word.state != MUTED) {
                word.copy(state = MUTED)
              } else {
                word
              }
            }
            NO_CHANGES -> {
              newWords -= word
              deletedWords -= word
              newWords += if (word.state != PRESENTED) word.copy(state = PRESENTED) else word
            }
          }
        } else if (wasInRange) break
      }
    }
    for (edition in editions) {
      processList(data.words, edition)
      processList(data.deletedWords, edition)
    }
    return TranscriptData.create(newWords.toList(), deletedWords.toList())
  }

  override fun dispose() {
    for (file in files) {
      FileEditorManager.getInstance(project).closeFile(file)
    }
    FILES.remove(file)
    myAudioEditionsListeners.clear()
    myAudioTranscriptListeners.clear()
    myAudioListeners.clear()
    myCodeListeners.clear()
  }

  private fun updateAudio(field: Audio?, value: Audio?) {
    if (value == null && field != null) {
      myAudioEditionsListeners.remove(field)
      myAudioTranscriptListeners.remove(field)
    }
    if (field == null && value != null) {
      installListeners(value as EditableAudio)
    }
  }

  private fun createVirtualFile(name: String, text: String, type: FileType): VirtualFile {
    return PsiFileFactory.getInstance(project).createFileFromText(
      name,
      type,
      text,
      0,
      true,
      false
    ).virtualFile
  }

  private fun installListeners(audio: EditableAudio) {
    addTranscriptListener(audio) {
      with(audio) {
        val data = data
        if (transcriptFile == null && data != null) {
          transcriptFile = createVirtualFile(
            "$name.transcript",
            data.text,
            TranscriptFileType
          ).also {
            it.putUserData(SCREENCAST_KEY, this@Screencast)
            it.putUserData(AUDIO_KEY, this)
          }
        }
        with(UndoManager.getInstance(project)) {
          if (!isRedoInProgress && !isUndoInProgress) {
            // But transcript should be updated always, otherwise it will cause errors.
            val nonNullData = data ?: return@addTranscriptListener
            transcriptPsi?.virtualFile?.updateDoc { doc ->
              with(PsiDocumentManager.getInstance(project)) {
                doPostponedOperationsAndUnblockDocument(doc)
                doc.setText(nonNullData.text)
                commitDocument(doc)
              }
            }
          }
        }
        val files = listOfNotNull(transcriptFile)
        PsiDocumentManager.getInstance(project).reparseFiles(files, true)
      }
    }
  }

  sealed class AudioState(val isPlugin: Boolean) {

    class NoState(isPlugin: Boolean) : AudioState(isPlugin) {
      override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NoState) return false
        return true
      }

      override fun hashCode(): Int {
        return javaClass.hashCode()
      }
    }

    data class ExistentState(
      val audio: EditableAudio,
      val editionsView: EditionsView,
      val offsetFrames: Long,
      var data: TranscriptData?
    ) : AudioState(audio.isPlugin)
  }

  abstract inner class Audio(
    val isPlugin: Boolean,
    open val model: AudioModel,
    open val editionsModel: EditionsView,
    open val data: TranscriptData?
  ) {
    val transcriptPsi: TranscriptPsiFile?
      get() = getPsi(project, transcriptFile)

    open val transcriptFile: VirtualFile? = null
    val audioInputStream: InputStream
      get() {
        return if (isPlugin) {
          myZip.audioInputStream
        } else if (importedAudioPath != null) {
          Files.newInputStream(importedAudioPath!!)
        } else {
          myZip.importedAudioInputStream
        }
      }

  }

  inner class EditableAudio(
    isPlugin: Boolean,
    override val model: ShiftableAudioModel,
    override val editionsModel: EditionsModel = DefaultEditionsModel(),
    override var data: TranscriptData? = null
  ) : Audio(isPlugin, model, editionsModel, data) {
    override var transcriptFile: VirtualFile? = null

    fun getState(): AudioState {
      return AudioState.ExistentState(this, editionsModel.copy(), model.offsetFrames, data)
    }

    fun renameWord(index: Int, text: String) {
      data = data?.rename(index, text)
    }

    fun changeRange(index: Int, newRange: IntRange) {
      val dataT = data ?: return
      val word = dataT[index]
      val newFrameRange = coordinator.toFrameRange(newRange, TimeUnit.MILLISECONDS)
      when (word.state) {
        MUTED -> {
          editionsModel.unmute(coordinator.toFrameRange(word.range, TimeUnit.MILLISECONDS))
          editionsModel.mute(newFrameRange)
        }
        PRESENTED -> Unit
      }
      data = dataT.replace(listOf(index to word.copy(range = newRange)))
    }

    fun concatenateWords(indexRange: IntRange) {
      data = data?.concatenate(indexRange)
    }

    fun excludeWords(indices: IntArray) {
      applyEdition(indices, EditionsModel::cut)
      data = data?.delete(indices)
    }

    fun excludeWord(index: Int) {
      data?.let { data ->
        editionsModel.cut(coordinator.toFrameRange(data.words[index].range, TimeUnit.MILLISECONDS))
      }
      data = data?.delete(index)
    }

    fun showWords(indices: IntArray) {
      applyEdition(indices, EditionsModel::unmute)
      data = data?.unmute(indices)
    }

    fun shiftAll(ms: Int) {
      val newWords = data?.words?.map { it.copy(range = it.range.shift(ms)) } ?: return
      val newDeletedWords = data?.deletedWords?.map { it.copy(range = it.range.shift(ms)) } ?: return
      data = TranscriptData.create(newWords, newDeletedWords)
    }

    fun muteWords(indices: IntArray) {
      applyEdition(indices, EditionsModel::mute)
      data = data?.mute(indices)
    }

    private fun applyEdition(indices: IntArray, action: EditionsModel.(LongRange) -> Unit) {
      data?.let { data ->
        for (i in indices) {
          editionsModel.action(coordinator.toFrameRange(data.words[i].range, TimeUnit.MILLISECONDS))
        }
      }
    }
  }

  private inner class ChangesReproducer : DocumentListener {
    private var myExpired = SimpleExpired()

    override fun documentChanged(event: DocumentEvent) {
      myExpired.isRunning = false
      myExpired = SimpleExpired()
      // If inside undo/redo action then text is set by the model, no model update required
      if (!myIsUndoRedoAction && !myForcedTextUpdate) {
        ApplicationManager.getApplication().invokeLater(Runnable {
          PsiDocumentManager.getInstance(project).performForCommittedDocument(scriptViewDoc!!) {
            if (!PsiTreeUtil.hasErrorElements(scriptViewPsi!!)) {
              val newCodes = codeModel.transformedByScript(scriptViewPsi!!).codes
              if (codeModel.codes != newCodes) {
                LOG.info("Code model changed, updating screencast")
                performModification {
                  codeModel.codes = newCodes
                  notifyCodeUpdatedByText()
                }
              }
            }
          }
        }, myExpired)
      }
    }
  }

  private inner class ScreencastUndoableAction(
    private val states: List<Pair<AudioState, AudioState>>
  ) :
    UndoableAction {

    private val myAffectedDocuments = mutableSetOf<DocumentReference>()

    init {
      files.forEach { myAffectedDocuments.add(DocumentReferenceManager.getInstance().create(it)) }
    }

    var codeUpdate: Pair<List<Code>, List<Code>>? = null
    var importedPathUpdate: Pair<Path?, Path?>? = null

    override fun redo() {
      performModification {
        states.map { it.second }.forEach { load(it) }
        codeUpdate?.let {
          myCodeModel.codes = it.second
        }
        importedPathUpdate?.let {
          importedAudioPath = it.second
        }
      }
    }

    override fun undo() {
      performModification {
        states.map { it.first }.forEach { load(it) }
        if (codeUpdate != null) {
          myCodeModel.codes = codeUpdate!!.first
        }
        importedPathUpdate?.let {
          importedAudioPath = it.first
        }
      }
    }

    private fun load(state: AudioState) {
      when (state) {
        is AudioState.NoState -> {
          if (state.isPlugin) {
            myEditablePluginAudio = null
          } else {
            myEditableImportedAudio = null
          }
        }
        is AudioState.ExistentState -> {
          if (state.isPlugin) {
            myEditablePluginAudio = state.audio
          } else {
            myEditableImportedAudio = state.audio
          }
          state.audio.data = state.data
          state.audio.model.offsetFrames = state.offsetFrames
          if (state.audio.editionsModel != state.editionsView) {
            state.audio.editionsModel.load(state.editionsView)
          }
        }
      }
    }

    override fun isGlobal(): Boolean {
      return false
    }

    override fun getAffectedDocuments(): Array<DocumentReference>? {
      return myAffectedDocuments.toTypedArray()
    }
  }

  inner class ModificationScope {
    val codeModel get() = myCodeModel
    val pluginEditableAudio get() = myEditablePluginAudio
    val importedEditableAudio get() = myEditableImportedAudio
    var isCodeUpdatedByText: Boolean = false
      private set

    fun getEditable(audio: Audio): EditableAudio {
      return if (audio.isPlugin) pluginEditableAudio!! else importedEditableAudio!!
    }

    fun importAudio(path: Path) {
      myEditableImportedAudio = EditableAudio(false, SimpleAudioModel { Files.newInputStream(path) })
      importedAudioPath = path
    }

    fun removeImportedAudio() {
      myEditableImportedAudio = null
      importedAudioPath = null
    }

    fun notifyCodeUpdatedByText() {
      isCodeUpdatedByText = true
    }
  }


  private class SimpleExpired : Condition<Any> {
    override fun value(t: Any?): Boolean = !isRunning
    var isRunning = true
  }

  companion object {
    private const val MAX_UNDO_ACTIONS = 15
    private val FILES: MutableMap<Path, Screencast> = ConcurrentHashMap()
    private val LOG = logger<Screencast>()

    val AUDIO_KEY = Key.create<Audio>("AUDIO_KEY")
    val SCREENCAST_KEY = Key.create<Screencast>("SCREENCAST_FILE_KEY")

    fun get(file: Path): Screencast? = FILES[file]

    suspend fun create(project: Project, file: Path): Screencast {
      if (FILES[file] != null) {
        throw IllegalStateException("Object associated with this file has been already created.")
      }
      val model = Screencast(project, file)
      model.initialize()
      FILES[file] = model
      return model
    }

    private inline fun <reified T : PsiFile> getPsi(project: Project, virtualFile: VirtualFile?): T? {
      val file = virtualFile ?: return null
      val psi = if (ApplicationManager.getApplication().isReadAccessAllowed) {
        val doc = FileDocumentManager.getInstance().getDocument(file) ?: return null
        PsiDocumentManager.getInstance(project).getPsiFile(doc)
      } else {
        runReadAction {
          val doc = FileDocumentManager.getInstance().getDocument(file) ?: return@runReadAction null
          PsiDocumentManager.getInstance(project).getPsiFile(doc)
        }
      }
      return psi as? T
    }

    private fun VirtualFile.updateDoc(action: (Document) -> Unit) {
      FileDocumentManager.getInstance().getDocument(this)?.let { doc ->
        ApplicationManager.getApplication().invokeAndWait {
          ApplicationManager.getApplication().runWriteAction {
            action(doc)
          }
        }
      }
    }
  }
}
