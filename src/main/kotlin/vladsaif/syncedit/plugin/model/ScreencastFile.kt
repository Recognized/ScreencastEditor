package vladsaif.syncedit.plugin.model

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.CommandProcessor
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
import org.jetbrains.kotlin.idea.debugger.readAction
import org.jetbrains.kotlin.psi.KtFile
import vladsaif.syncedit.plugin.actions.errorScriptContainsErrors
import vladsaif.syncedit.plugin.editor.Coordinator
import vladsaif.syncedit.plugin.editor.audioview.waveform.AudioDataModel
import vladsaif.syncedit.plugin.editor.audioview.waveform.impl.SimpleAudioModel
import vladsaif.syncedit.plugin.format.ScreencastFileType
import vladsaif.syncedit.plugin.format.ScreencastZipper
import vladsaif.syncedit.plugin.format.ScreencastZipper.Companion.getInputStreamByType
import vladsaif.syncedit.plugin.format.ScreencastZipper.Companion.isDataSet
import vladsaif.syncedit.plugin.format.ScreencastZipper.EntryType.*
import vladsaif.syncedit.plugin.format.transferTo
import vladsaif.syncedit.plugin.lang.script.psi.Code
import vladsaif.syncedit.plugin.lang.script.psi.CodeModel
import vladsaif.syncedit.plugin.lang.script.psi.CodeModelView
import vladsaif.syncedit.plugin.lang.script.psi.TimeOffsetParser
import vladsaif.syncedit.plugin.lang.transcript.fork.TranscriptFactoryListener
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptFileType
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptPsiFile
import vladsaif.syncedit.plugin.model.WordData.State.MUTED
import vladsaif.syncedit.plugin.model.WordData.State.PRESENTED
import vladsaif.syncedit.plugin.sound.EditionModel
import vladsaif.syncedit.plugin.sound.EditionModel.EditionType.*
import vladsaif.syncedit.plugin.sound.EditionModelView
import vladsaif.syncedit.plugin.sound.impl.DefaultEditionModel
import vladsaif.syncedit.plugin.util.ExEDT
import vladsaif.syncedit.plugin.util.contains
import vladsaif.syncedit.plugin.util.shift
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

class ScreencastFile(
  val project: Project,
  val file: Path
) : Disposable {

  private val myTranscriptListeners: MutableSet<() -> Unit> = ContainerUtil.newConcurrentSet()
  private val myEditionListeners: MutableSet<() -> Unit> = ContainerUtil.newConcurrentSet()
  private val myCodeListeners: MutableSet<() -> Unit> = ContainerUtil.newConcurrentSet()
  private val isTranscriptSet: Boolean by lazy { isDataSet(file, TRANSCRIPT_DATA) }
  private val isScriptSet: Boolean by lazy { isDataSet(file, SCRIPT) }
  private val isEditionModelSet: Boolean by lazy { isDataSet(file, EDITION_MODEL) }
  private val myTranscriptInputStream: InputStream
    get() = getInputStreamByType(file, TRANSCRIPT_DATA) ?: throw IllegalStateException("Transcript is not set")
  private val myScriptInputStream: InputStream
    get() = getInputStreamByType(file, SCRIPT) ?: throw IllegalStateException("Script is not set")
  private val myEditionModelInputStream: InputStream
    get() = getInputStreamByType(file, EDITION_MODEL) ?: throw IllegalStateException("Edition model is not set")
  private val myEditionModel: EditionModel = DefaultEditionModel()
  private val myCodeModel = CodeModel(listOf())
  private var isInsideModification: Boolean = false
  private var isUndoRedoAction: Boolean = false
  private val myUndoStack = ArrayDeque<ScreencastUndoableAction>()
  private val myRedoStack = ArrayDeque<ScreencastUndoableAction>()
  val editionModel: EditionModelView = myEditionModel
  val codeModel: CodeModelView = myCodeModel
  val name: String
    get() = file.fileName.toString().substringBefore(ScreencastFileType.dotExtension)
  var audioDataModel: AudioDataModel? = null
    private set
  val audioInputStream: InputStream
    get() = getInputStreamByType(file, AUDIO) ?: throw IllegalStateException("Audio is not set")
  val isAudioSet: Boolean by lazy { isDataSet(file, AUDIO) }
  val transcriptPsi: TranscriptPsiFile?
    get() = getPsi(project, transcriptFile)
  var transcriptFile: VirtualFile? = null
    private set
  private var scriptFile: VirtualFile? = null
  val coordinator: Coordinator = Coordinator()
  var scriptViewFile: VirtualFile? = null
    private set
  val scriptViewDoc: Document?
    get() = scriptViewPsi?.viewProvider?.document
  val scriptViewPsi: KtFile?
    get() = getPsi(project, scriptViewFile)
  val scriptDocument: Document?
    get() = scriptPsi?.viewProvider?.document
  val scriptPsi: KtFile?
    get() = getPsi(project, scriptFile)
  var data: TranscriptData? = null
    private set

  private var fakeFile: VirtualFile? = null

  init {
    if (!file.exists()) {
      throw IOException("File ($file) does not exist.")
    }
    if (!file.isFile() || !file.toString().endsWith(ScreencastFileType.dotExtension)) {
      throw IOException("Supplied file ($file) is not screencast.")
    }
    EditorFactory.getInstance().addEditorFactoryListener(TranscriptFactoryListener(), this)
  }

  val isUndoAvailable: Boolean
    get() = !myUndoStack.isEmpty()

  val isRedoAvailable: Boolean
    get() = !myRedoStack.isEmpty()

  fun undo() {
    undoOrRedo(true)
  }

  fun redo() {
    undoOrRedo(false)
  }

  private fun undoOrRedo(isUndo: Boolean) {
    val action = (if (isUndo) myUndoStack.pollLast() else myRedoStack.pollLast())
      ?: throw IllegalStateException("Undo/Redo is not available")
    isUndoRedoAction = true
    if (isUndo) {
      action.undo()
      myRedoStack.clear()
      myRedoStack.addLast(action)
    } else {
      action.redo()
      myUndoStack.addLast(action)
    }
    isUndoRedoAction = false
  }

  @Synchronized
  fun performModification(action: ModificationScope.() -> Unit) {
    if (isInsideModification) {
      ModificationScope().action()
      return
    }
    isInsideModification = true
    val dataBefore = data
    val editionModelBefore = myEditionModel.copy()
    val codesBefore = codeModel.codes
    val offsetFramesBefore = audioDataModel?.offsetFrames
    ModificationScope().action()
    val editionModelAfter = myEditionModel
    if (dataBefore != null && dataBefore == data && editionModelBefore != editionModelAfter) {
      data = synchronizeWithEditionModel(dataBefore)
    }
    val offsetFramesAfter = audioDataModel?.offsetFrames
    val dataAfter = data
    val undoableAction = ScreencastUndoableAction()
    val codesAfter = codeModel.codes
    if (offsetFramesBefore != offsetFramesAfter) {
      audioDataModel?.let { undoableAction.offsetFramesChange[it] = offsetFramesBefore!! to offsetFramesAfter!! }
    }
    if (dataBefore != dataAfter) {
      undoableAction.transcriptUpdate = dataBefore to dataAfter
      myTranscriptListeners.forEach { it() }
    }
    if (codesBefore != codesAfter) {
      undoableAction.codeUpdate = codesBefore to codesAfter
      myCodeListeners.forEach { it() }
    }
    if (editionModelBefore != editionModelAfter) {
      undoableAction.editionModelUpdate = editionModelBefore to editionModelAfter
      myEditionListeners.forEach { it() }
    }
    if (CommandProcessor.getInstance().currentCommand != null) {
      with(UndoManager.getInstance(project)) {
        if (!isRedoInProgress && !isUndoInProgress) {
          undoableActionPerformed(undoableAction)
        }
      }
    }
    if (!isUndoRedoAction) {
      myRedoStack.clear()
      myUndoStack.addLast(undoableAction)
      if (myUndoStack.size > MAX_UNDO_ACTIONS) {
        myUndoStack.removeFirst()
      }
    }
    isInsideModification = false
  }

  private fun initializeEditionModel() {
    val output = ByteArrayOutputStream()
    myEditionModelInputStream.use {
      it.transferTo(output)
    }
    val modelFromZip = EditionModel.deserialize(output.toByteArray())
    for ((range, type) in modelFromZip.editions) {
      when (type) {
        CUT -> myEditionModel.cut(range)
        MUTE -> myEditionModel.mute(range)
        NO_CHANGES -> myEditionModel.unmute(range)
      }
    }
  }

  private fun initializeScript() {
    val tempFile = createVirtualFile(
      "$name.kts",
      readContents(myScriptInputStream),
      KotlinFileType.INSTANCE
    ).also { it.putUserData(KEY, this@ScreencastFile) }
    PsiDocumentManager.getInstance(project).commitDocument(getPsi<KtFile>(project, tempFile)!!.viewProvider.document!!)
    if (!PsiTreeUtil.hasErrorElements(getPsi<KtFile>(project, tempFile)!!)) {
      myCodeModel.codes = TimeOffsetParser.parse(getPsi(project, tempFile)!!).codes
    } else {
      errorScriptContainsErrors(this@ScreencastFile)
      // TODO
    }
    scriptFile = createVirtualFile(
      "$name.kts",
      codeModel.serialize(),
      KotlinFileType.INSTANCE
    )
    PsiDocumentManager.getInstance(project).commitDocument(scriptDocument!!)
    val markedText = myCodeModel.createTextWithoutOffsets()
    scriptViewFile = createVirtualFile(
      "$name.kts",
      markedText.text,
      KotlinFileType.INSTANCE
    )
    scriptViewDoc!!.addDocumentListener(ChangesReproducer())
  }

  private fun initializeTranscript() {
    val newData = myTranscriptInputStream.let { TranscriptData.createFrom(it) }
    data = if (isEditionModelSet) newData
    else synchronizeWithEditionModel(newData)
  }

  suspend fun initialize() {
    withContext(Dispatchers.Default) {
      if (isAudioSet) {
        audioDataModel = SimpleAudioModel { audioInputStream }
      }
    }
    withContext(ExEDT) {
      if (isAudioSet) {
        coordinator.framesPerSecond = (audioDataModel!!.framesPerMillisecond * 1000).toLong()
      }
      if (isEditionModelSet) {
        initializeEditionModel()
      }
      if (isScriptSet) {
        initializeScript()
      }
      if (isTranscriptSet) {
        initializeTranscript()
      }
      fakeFile = createVirtualFile("empty", "", XmlFileType.INSTANCE)
      installListeners()
    }
  }

  private fun installListeners() {
    addTranscriptListener {
      // Synchronize edition model with transcript data if it was changed in editor.
      // Also do not forget to reset coordinates cache.
      val data = data
      if (transcriptPsi == null && data != null) {
        transcriptFile = createVirtualFile(
          "$name.transcript",
          data.text,
          TranscriptFileType
        )
        transcriptFile!!.putUserData(KEY, this)
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
      val files = listOfNotNull(transcriptFile, scriptFile)
      PsiDocumentManager.getInstance(project).reparseFiles(files, true)
    }
  }

  /**
   * Hard save function rewrites audio file and discards information about script and transcript changes.
   *
   * @return function that saves this screencast in the state when [getHardSaveFunction] was called
   */
//  fun getHardSaveFunction(): (progressUpdater: (Double) -> Unit, Path) -> Unit {
//    val editionState = myEditionModel.copy()
//    val msDeleted = IntRangeUnion()
//    if (isAudioSet) {
//      for ((range, type) in editionState.editions) {
//        if (type == CUT) {
//          msDeleted.union(coordinator.toMillisecondsRange(range))
//        }
//      }
//    }
//     Change word ranges because of deletions
//    val newTranscriptData = data?.words?.asSequence()
//      ?.map { it.copy(range = msDeleted.impose(it.range)) }
//      ?.filter { !it.range.empty }
//      ?.toList()
//      ?.let { TranscriptData(it, listOf()) }
//
//     TODO update offsets
//    val newScript = scriptDocument?.text
//
//    return { progressUpdater, out ->
//      val tempFile = Files.createTempFile("screencast", "." + ScreencastFileType.defaultExtension)
//      ScreencastZipper(tempFile).use { zipper ->
//        if (isAudioSet) {
//          zipper.addAudio(Supplier { audioInputStream }, editionState, progressUpdater)
//        }
//        newTranscriptData?.let(zipper::addTranscriptData)
//        newScript?.let(zipper::addScript)
//      }
//      Files.move(tempFile, out, StandardCopyOption.REPLACE_EXISTING)
//    }
//  }

  fun getLightSaveFunction(): (Path) -> Unit {
    val newTranscriptData = data
    val newScript = readAction { scriptDocument?.text }
    val newEditionModel = myEditionModel.copy()

    return { out ->
      val tempFile = Files.createTempFile("screencast", "." + ScreencastFileType.defaultExtension)
      ScreencastZipper(tempFile).use { zipper ->
        if (isAudioSet) {
          zipper.addAudio(audioInputStream)
        }
        newTranscriptData?.let(zipper::addTranscriptData)
        newScript?.let(zipper::addScript)
        zipper.addEditionModel(newEditionModel)
      }
      Files.move(tempFile, out, StandardCopyOption.REPLACE_EXISTING)
    }
  }

  fun addTranscriptListener(listener: () -> Unit) {
    myTranscriptListeners += listener
  }

  fun removeTranscriptListener(listener: () -> Unit) {
    myTranscriptListeners -= listener
  }

  fun addCodeListener(listener: () -> Unit) {
    myCodeListeners += listener
  }

  fun removeCodeListener(listener: () -> Unit) {
    myCodeListeners -= listener
  }

  fun addEditionListener(listener: () -> Unit) {
    myEditionListeners += listener
  }

  fun removeEditionListener(listener: () -> Unit) {
    myEditionListeners -= listener
  }


//  private fun updateRangeHighlighters() {
//    val document = scriptDocument ?: return
//    for (editor in EditorFactory.getInstance().getEditors(document)) {
//      for ((_, marker) in textMapping) {
//        (editor as EditorEx).markupModel.addRangeHighlighter(
//            marker.startOffset,
//            marker.endOffset,
//            10000,
//            editor.colorsScheme.getAttributes(EditorColors.DELETED_TEXT_ATTRIBUTES),
//            HighlighterTargetArea.EXACT_RANGE
//        )
//      }
//    }
//  }

  private fun synchronizeWithEditionModel(data: TranscriptData): TranscriptData {
    val editions =
      myEditionModel.editions.map { coordinator.toMillisecondsRange(it.first) to it.second }
    val newWords = data.words.toMutableSet()
    val deletedWords = data.deletedWords.toMutableSet()
    val processList = { list: List<WordData>, edition: Pair<IntRange, EditionModel.EditionType> ->
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
    return TranscriptData(newWords.toList(), deletedWords.toList())
  }

  override fun dispose() {
    FILES.remove(file)
    myTranscriptListeners.clear()
    myEditionListeners.clear()
    myCodeListeners.clear()
  }

  override fun toString(): String {
    return "ScreencastFile(file=$file, data=$data, scriptPsi=$scriptPsi, " +
        "transcriptPsi=$transcriptPsi, audioDataModel=$audioDataModel, myEditionModel=$myEditionModel)"
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


  private inner class ChangesReproducer : DocumentListener {
    private var myExpired = SimpleExpired()

    override fun documentChanged(event: DocumentEvent) {
      myExpired.isRunning = false
      myExpired = SimpleExpired()
      ApplicationManager.getApplication().invokeLater(Runnable {
        PsiDocumentManager.getInstance(project).performForCommittedDocument(scriptViewDoc!!) {
          if (!PsiTreeUtil.hasErrorElements(scriptViewPsi!!)) {
            performModification {
              codeModel.codes = codeModel.transformedByScript(scriptViewPsi!!).codes
            }
          }
        }
      }, myExpired)
    }
  }


  private inner class ScreencastUndoableAction : UndoableAction {
    private val myAffectedDocuments = mutableSetOf<DocumentReference>()

    init {
      transcriptFile
        ?.let { myAffectedDocuments.add(DocumentReferenceManager.getInstance().create(it)) }
      scriptFile
        ?.let { myAffectedDocuments.add(DocumentReferenceManager.getInstance().create(it)) }
      scriptViewFile
        ?.let { myAffectedDocuments.add(DocumentReferenceManager.getInstance().create(it)) }
    }

    val offsetFramesChange: MutableMap<AudioDataModel, Pair<Long, Long>> = mutableMapOf()
    var transcriptUpdate: Pair<TranscriptData?, TranscriptData?>? = null
    var editionModelUpdate: Pair<EditionModel, EditionModel>? = null
    var codeUpdate: Pair<List<Code>, List<Code>>? = null

    override fun redo() {
      performModification {
        if (transcriptUpdate != null) {
          data = transcriptUpdate!!.second
        }
        if (editionModelUpdate != null) {
          myEditionModel.load(editionModelUpdate!!.second)
        }
        if (codeUpdate != null) {
          myCodeModel.codes = codeUpdate!!.second
        }
        for ((model, change) in offsetFramesChange) {
          val (_, new) = change
          model.offsetFrames = new
        }
      }
    }

    override fun undo() {
      performModification {
        if (transcriptUpdate != null) {
          data = transcriptUpdate!!.first
        }
        if (editionModelUpdate != null) {
          myEditionModel.load(editionModelUpdate!!.first)
        }
        if (codeUpdate != null) {
          myCodeModel.codes = codeUpdate!!.first
        }
        for ((model, change) in offsetFramesChange) {
          val (old, _) = change
          model.offsetFrames = old
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
    val editionModel get() = myEditionModel
    val codeModel get() = myCodeModel

    fun setTranscriptData(newData: TranscriptData?) {
      data = newData
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
          myEditionModel.unmute(coordinator.toFrameRange(word.range, TimeUnit.MILLISECONDS))
          myEditionModel.mute(newFrameRange)
        }
        PRESENTED -> Unit
      }
      data = dataT.replace(listOf(index to word.copy(range = newRange)))
    }

    fun concatenateWords(indexRange: IntRange) {
      data = data?.concatenate(indexRange)
    }

    fun excludeWords(indices: IntArray) {
      applyEdition(indices, EditionModel::cut)
      data = data?.delete(indices)
    }

    fun excludeWord(index: Int) {
      data?.let { data ->
        editionModel.cut(coordinator.toFrameRange(data.words[index].range, TimeUnit.MILLISECONDS))
      }
      data = data?.delete(index)
    }

    fun showWords(indices: IntArray) {
      applyEdition(indices, EditionModel::unmute)
      data = data?.unmute(indices)
    }

    fun shiftAll(ms: Int) {
      val newWords = data?.words?.map { it.copy(range = it.range.shift(ms)) } ?: return
      val newDeletedWords = data?.deletedWords?.map { it.copy(range = it.range.shift(ms)) } ?: return
      data = TranscriptData(newWords, newDeletedWords)
    }

    fun muteWords(indices: IntArray) {
      applyEdition(indices, EditionModel::mute)
      data = data?.mute(indices)
    }

    private fun applyEdition(indices: IntArray, action: EditionModel.(LongRange) -> Unit) {
      data?.let { data ->
        for (i in indices) {
          myEditionModel.action(coordinator.toFrameRange(data.words[i].range, TimeUnit.MILLISECONDS))
        }
      }
    }
  }


  private class SimpleExpired : Condition<Any> {
    override fun value(t: Any?): Boolean = !isRunning
    var isRunning = true
  }


  companion object {
    private const val MAX_UNDO_ACTIONS = 15
    private val FILES: MutableMap<Path, ScreencastFile> = ConcurrentHashMap()
    private val LOG = logger<ScreencastFile>()
    val KEY = Key.create<ScreencastFile>("SCREENCAST_FILE_KEY")

    fun get(file: Path): ScreencastFile? = FILES[file]

    suspend fun create(project: Project, file: Path): ScreencastFile {
      if (FILES[file] != null) {
        throw IllegalStateException("Object associated with this file has been already created.")
      }
      val model = ScreencastFile(project, file)
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

    private fun readContents(stream: InputStream): String {
      return stream.bufferedReader(Charset.forName("UTF-8")).use {
        it.lines().collect(Collectors.joining("\n"))
      }
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
