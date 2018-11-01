package vladsaif.syncedit.plugin.model

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
import org.jetbrains.kotlin.psi.KtFile
import vladsaif.syncedit.plugin.actions.errorScriptContainsErrors
import vladsaif.syncedit.plugin.editor.audioview.waveform.AudioDataModel
import vladsaif.syncedit.plugin.editor.audioview.waveform.impl.SimpleAudioModel
import vladsaif.syncedit.plugin.format.ScreencastFileType
import vladsaif.syncedit.plugin.format.ScreencastZipper
import vladsaif.syncedit.plugin.format.ScreencastZipper.Companion.getInputStreamByType
import vladsaif.syncedit.plugin.format.ScreencastZipper.Companion.isDataSet
import vladsaif.syncedit.plugin.format.ScreencastZipper.EntryType.*
import vladsaif.syncedit.plugin.format.transferTo
import vladsaif.syncedit.plugin.lang.script.psi.CodeModel
import vladsaif.syncedit.plugin.lang.script.psi.TimeOffsetParser
import vladsaif.syncedit.plugin.lang.transcript.fork.TranscriptFactoryListener
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptFileType
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptPsiFile
import vladsaif.syncedit.plugin.model.WordData.State.*
import vladsaif.syncedit.plugin.sound.EditionModel
import vladsaif.syncedit.plugin.sound.EditionModel.EditionType.*
import vladsaif.syncedit.plugin.sound.impl.DefaultEditionModel
import vladsaif.syncedit.plugin.util.ExEDT
import vladsaif.syncedit.plugin.util.IntRangeUnion
import vladsaif.syncedit.plugin.util.contains
import vladsaif.syncedit.plugin.util.empty
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier
import java.util.stream.Collectors
import javax.swing.event.ChangeListener

/**
 * Main class that holds information about transcript data,
 * changes that were made of it or of audio data.
 *
 * @constructor
 * @throws java.io.IOException If I/O error occurs while reading xml
 */
class ScreencastFile(
  val project: Project,
  val file: Path
) : Disposable {

  private val myListeners: MutableSet<() -> Unit> = ContainerUtil.newConcurrentSet()
  private var myEditionListenerEnabled = true
  private var myTranscriptListenerEnabled = true
  private val myTranscriptInputStream: InputStream
    get() = getInputStreamByType(file, TRANSCRIPT_DATA) ?: throw IllegalStateException("Transcript is not set")
  private val isTranscriptSet: Boolean
    get() = isDataSet(file, TRANSCRIPT_DATA)
  private val myScriptInputStream: InputStream
    get() = getInputStreamByType(file, SCRIPT) ?: throw IllegalStateException("Script is not set")
  private val isScriptSet: Boolean
    get() = isDataSet(file, SCRIPT)
  private val isEditionModelSet: Boolean
    get() = isDataSet(file, EDITION_MODEL)
  private val myEditionModelInputStream: InputStream
    get() = getInputStreamByType(file, EDITION_MODEL) ?: throw IllegalStateException("Edition model is not set")
  val name: String
    get() = file.fileName.toString().substringBefore(ScreencastFileType.dotExtension)
  var audioDataModel: AudioDataModel? = null
    private set
  val audioInputStream: InputStream
    get() = getInputStreamByType(file, AUDIO) ?: throw IllegalStateException("Audio is not set")
  val codeModel = CodeModel(listOf())
  val isAudioSet: Boolean
    get() = isDataSet(file, AUDIO)
  val transcriptPsi: TranscriptPsiFile?
    get() = getPsi(project, transcriptFile)
  var transcriptFile: VirtualFile? = null
    private set
  private var scriptFile: VirtualFile? = null
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
  val editionModel: EditionModel = DefaultEditionModel()
  var data: TranscriptData? = null
    private set(value) {
      ApplicationManager.getApplication().assertIsDispatchThread()
      if (value != field) {
        if (CommandProcessor.getInstance().currentCommand != null
          && !UndoManager.getInstance(project).isUndoInProgress
          && !UndoManager.getInstance(project).isRedoInProgress
        ) {
          UndoManager.getInstance(project).undoableActionPerformed(TranscriptDataUndoableAction(field!!, value))
        }
        field = value
        fireTranscriptDataChanged()
      }
    }

  init {
    if (!file.exists()) {
      throw IOException("File ($file) does not exist.")
    }
    if (!file.isFile() || !file.toString().endsWith(ScreencastFileType.dotExtension)) {
      throw IOException("Supplied file ($file) is not screencast.")
    }
    EditorFactory.getInstance().addEditorFactoryListener(TranscriptFactoryListener(), this)
  }

  fun loadTranscriptData(newData: TranscriptData) {
    data = newData
    bulkChange {
      val words = data?.words ?: return@bulkChange
      val audio = audioDataModel ?: return@bulkChange
      editionModel.reset()
      for (word in words) {
        when (word.state) {
          EXCLUDED -> editionModel.cut(audio.msRangeToFrameRange(word.range))
          MUTED -> editionModel.mute(audio.msRangeToFrameRange(word.range))
          PRESENTED -> editionModel.undo(audio.msRangeToFrameRange(word.range))
        }
      }
    }
  }

  private fun initializeEditionModel() {
    val output = ByteArrayOutputStream()
    myEditionModelInputStream.use {
      it.transferTo(output)
    }
    val modelFromZip = EditionModel.deserialize(output.toByteArray())
    for ((range, type) in modelFromZip.editions) {
      when (type) {
        CUT -> editionModel.cut(range)
        MUTE -> editionModel.mute(range)
        NO_CHANGES -> editionModel.undo(range)
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
      codeModel.blocks = TimeOffsetParser.parse(getPsi(project, tempFile)!!).blocks
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
    val markedText = codeModel.createTextWithoutOffsets()
    scriptViewFile = createVirtualFile(
      "$name.kts",
      markedText.text,
      KotlinFileType.INSTANCE
    )
    scriptViewDoc!!.addDocumentListener(ChangesReproducer())
  }

  private fun initializeTranscript() {
    val newData = myTranscriptInputStream.let { TranscriptData.createFrom(it) }
    if (isEditionModelSet) data = newData
    else loadTranscriptData(newData)
  }

  suspend fun initialize() {
    withContext(Dispatchers.Default) {
      if (isAudioSet) {
        audioDataModel = SimpleAudioModel { audioInputStream }
      }
    }
    withContext(ExEDT) {
      if (isEditionModelSet) {
        initializeEditionModel()
      }
      if (isScriptSet) {
        initializeScript()
      }
      if (isTranscriptSet) {
        initializeTranscript()
      }
      installListeners()
    }
  }

  private fun installListeners() {
    addTranscriptDataListener {
      val files = listOfNotNull(transcriptFile, scriptFile)
      PsiDocumentManager.getInstance(project).reparseFiles(files, true)
    }
    editionModel.addChangeListener(ChangeListener {
      if (myEditionListenerEnabled) {
        onEditionModelChanged()
      }
    })
  }

  /**
   * Hard save function rewrites audio file and discards information about script and transcript changes.
   *
   * @return function that saves this screencast in the state when [getHardSaveFunction] was called
   */
  fun getHardSaveFunction(): (progressUpdater: (Double) -> Unit, Path) -> Unit {
    val editionState = editionModel.copy()
    val msDeleted = IntRangeUnion()
    if (isAudioSet) {
      for ((range, type) in editionState.editions) {
        if (type == CUT) {
          msDeleted.union(audioDataModel!!.frameRangeToMsRange(range))
        }
      }
    }
    // Change word ranges because of deletions
    val newTranscriptData = data?.words?.asSequence()
      ?.filter { it.state != WordData.State.EXCLUDED }
      ?.map { it.copy(range = msDeleted.impose(it.range)) }
      ?.filter { !it.range.empty }
      ?.toList()
      ?.let { TranscriptData(it) }

    // TODO update offsets
    val newScript = scriptDocument?.text

    return { progressUpdater, out ->
      val tempFile = Files.createTempFile("screencast", "." + ScreencastFileType.defaultExtension)
      ScreencastZipper(tempFile).use { zipper ->
        if (isAudioSet) {
          zipper.addAudio(Supplier { audioInputStream }, editionState, progressUpdater)
        }
        newTranscriptData?.let(zipper::addTranscriptData)
        newScript?.let(zipper::addScript)
      }
      Files.move(tempFile, out, StandardCopyOption.REPLACE_EXISTING)
    }
  }

  fun getLightSaveFunction(): (Path) -> Unit {
    val newTranscriptData = data
    val newScript = scriptDocument?.text
    val newEditionModel = editionModel.copy()

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

  fun addTranscriptDataListener(listener: () -> Unit) {
    myListeners += listener
  }

  fun removeTranscriptDataListener(listener: () -> Unit) {
    myListeners -= listener
  }

  private fun replaceWords(replacements: List<Pair<Int, WordData>>) {
    if (replacements.isEmpty()) return
    data = data?.replaceWords(replacements)
  }

  fun renameWord(index: Int, text: String) {
    data = data?.renameWord(index, text)
  }

  fun changeRange(index: Int, newRange: IntRange) {
    val word = data?.get(index) ?: return
    val newFrameRange = audioDataModel!!.msRangeToFrameRange(newRange)
    bulkChange {
      when (word.state) {
        EXCLUDED, MUTED -> {
          undo(audioDataModel!!.msRangeToFrameRange(word.range))
          if (word.state == EXCLUDED) cut(newFrameRange)
          else mute(newFrameRange)
        }
        PRESENTED -> Unit
      }
    }
    data = data?.replaceWords(listOf(index to word.copy(range = newRange)))
  }

  fun concatenateWords(indexRange: IntRange) {
    data = data?.concatenateWords(indexRange)
  }

  fun excludeWords(indices: IntArray) {
    applyEdition(indices, EditionModel::cut)
    data = data?.excludeWords(indices)
  }

  fun excludeWord(index: Int) {
    data?.let { data ->
      bulkChange {
        cut(audioDataModel!!.msRangeToFrameRange(data.words[index].range))
      }
    }
    data = data?.excludeWord(index)
  }

  fun showWords(indices: IntArray) {
    applyEdition(indices, EditionModel::undo)
    data = data?.showWords(indices)
  }

  fun muteWords(indices: IntArray) {
    applyEdition(indices, EditionModel::mute)
    data = data?.muteWords(indices)
  }

  private fun applyEdition(indices: IntArray, action: EditionModel.(LongRange) -> Unit) {
    data?.let { data ->
      bulkChange {
        for (i in indices) {
          action(audioDataModel!!.msRangeToFrameRange(data.words[i].range))
        }
      }
    }
  }

  private fun bulkChange(action: EditionModel.() -> Unit) {
    try {
      editionModel.isNotificationSuppressed = true
      myEditionListenerEnabled = false
      editionModel.action()
    } finally {
      editionModel.isNotificationSuppressed = false
      editionModel.fireStateChanged()
      myEditionListenerEnabled = true
    }
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

  private fun onEditionModelChanged() {
    val preparedEditions = data?.let { getWordReplacements(it) } ?: return
    myTranscriptListenerEnabled = false
    try {
      replaceWords(preparedEditions)
    } finally {
      myTranscriptListenerEnabled = true
    }
  }

  private fun getWordReplacements(data: TranscriptData): List<Pair<Int, WordData>> {
    val audio = audioDataModel ?: return listOf()
    val editions = editionModel.editions.map { audio.frameRangeToMsRange(it.first) to it.second.toWordDataState() }
    val preparedEditions = mutableListOf<Pair<Int, WordData>>()
    for (edition in editions) {
      for ((i, word) in data.words.withIndex()) {
        if (word.range in edition.first && word.state != edition.second) {
          preparedEditions.add(i to word.copy(state = edition.second))
        }
      }
    }
    return preparedEditions.toList()
  }

  private fun updateTranscript() {
    val nonNullData = data ?: return
    transcriptPsi?.virtualFile?.updateDoc { doc ->
      with(PsiDocumentManager.getInstance(project)) {
        doPostponedOperationsAndUnblockDocument(doc)
        doc.setText(nonNullData.text)
        commitDocument(doc)
      }
    }
  }

  private fun fireTranscriptDataChanged() {
    // Synchronize edition model with transcript data if it was changed in editor.
    // Also do not forger to reset coordinates cache.
    if (myTranscriptListenerEnabled) {
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
          updateTranscript()
        }
      }
    }
    for (x in myListeners) {
      x.invoke()
    }
  }

  override fun dispose() {
    FILES.remove(file)
    myListeners.clear()
    data = null
  }

  override fun toString(): String {
    return "ScreencastFile(file=$file, myListeners=$myListeners, data=$data, scriptPsi=$scriptPsi, " +
        "transcriptPsi=$transcriptPsi, audioDataModel=$audioDataModel, editionModel=$editionModel)"
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

  private inner class TranscriptDataUndoableAction(
    private val dataBefore: TranscriptData?,
    private val dataAfter: TranscriptData?
  ) : UndoableAction {

    private val myAffectedDocuments = mutableSetOf<DocumentReference>()

    init {
      transcriptFile
        ?.let { FileDocumentManager.getInstance().getDocument(it) }
        ?.let { myAffectedDocuments.add(DocumentReferenceManager.getInstance().create(it)) }
      scriptDocument
        ?.let { myAffectedDocuments.add(DocumentReferenceManager.getInstance().create(it)) }
    }

    override fun redo() {
      data = dataAfter
    }

    override fun undo() {
      data = dataBefore
    }

    override fun isGlobal() = false

    override fun getAffectedDocuments() = myAffectedDocuments.toTypedArray()
  }

  private inner class ChangesReproducer : DocumentListener {
    private var myExpired = SimpleExpired()

    override fun documentChanged(event: DocumentEvent) {
      myExpired.isRunning = false
      myExpired = SimpleExpired()
      ApplicationManager.getApplication().invokeLater(Runnable {
        PsiDocumentManager.getInstance(project).performForCommittedDocument(scriptViewDoc!!) {
          if (!PsiTreeUtil.hasErrorElements(scriptViewPsi!!)) {
            codeModel.blocks = codeModel.transformedByScript(scriptViewPsi!!).blocks
            println(codeModel)
          }
        }
      }, myExpired)
    }
  }

  private class SimpleExpired : Condition<Any> {
    override fun value(t: Any?): Boolean = !isRunning
    var isRunning = true
  }

  companion object {
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

    private fun EditionModel.EditionType.toWordDataState() = when (this) {
      CUT -> EXCLUDED
      MUTE -> MUTED
      NO_CHANGES -> PRESENTED
    }
  }
}
