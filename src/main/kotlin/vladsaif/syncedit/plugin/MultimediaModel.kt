package vladsaif.syncedit.plugin

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.psi.KtFile
import vladsaif.syncedit.plugin.WordData.State.*
import vladsaif.syncedit.plugin.audioview.waveform.AudioDataModel
import vladsaif.syncedit.plugin.audioview.waveform.EditionModel
import vladsaif.syncedit.plugin.audioview.waveform.EditionModel.EditionType.*
import vladsaif.syncedit.plugin.audioview.waveform.impl.DefaultEditionModel
import vladsaif.syncedit.plugin.audioview.waveform.impl.SimpleAudioModel
import vladsaif.syncedit.plugin.lang.script.psi.TimeOffsetParser
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptPsiFile
import java.io.File
import javax.swing.event.ChangeListener

/**
 * Main class that holds information about transcript data,
 * changes that were made of it or of audio data.
 *
 * @constructor
 * @throws java.io.IOException If I/O error occurs while reading xml
 */
class MultimediaModel(
    val project: Project
) : Disposable {
  private val myListeners: MutableSet<Listener> = mutableSetOf()
  private var myEditionListenerEnabled = true
  private val myEditionModelListener = ChangeListener {
    if (myEditionListenerEnabled) {
      onEditionModelChanged()
    }
  }
  private var myTranscriptListenerEnabled = true
  private val myTranscriptDataListener = object : MultimediaModel.Listener {
    override fun onTranscriptDataChanged() {
      if (myTranscriptListenerEnabled) {
        with(UndoManager.getInstance(project)) {
          if (!isRedoInProgress && !isUndoInProgress) {
            // But transcript should be updated always, otherwise it will cause errors.
            updateTranscript()
          }
        }
        editionModel.isNotificationSuppressed = true
        synchronizeTranscriptWithEditionModel()
        editionModel.isNotificationSuppressed = false
        try {
          myEditionListenerEnabled = false
          editionModel.fireStateChanged()
        } finally {
          myEditionListenerEnabled = true
        }
      }
    }
  }
  var isNeedInitialBind: Boolean = true
  val editionModel: EditionModel = DefaultEditionModel()
  var audioDataModel: AudioDataModel? = null
    private set(value) {
      if (value != field) {
        field = value
        synchronizeTranscriptWithEditionModel()
      }
    }
  var xmlFile: VirtualFile? = null
    set(value) {
      if (value == field) return
      setDependencies(field, value)
      field = value
    }
  var audioFile: VirtualFile? = null
    set(value) {
      if (value == field) return
      setDependencies(field, value)
      field = value
      if (value != null) {
        audioDataModel = SimpleAudioModel(File(value.path).toPath())
      }
    }
  var scriptFile: VirtualFile? = null
    set(value) {
      if (value == field) return
      setDependencies(field, value)
      field = value
    }
  val scriptPsi: KtFile?
    get() {
      val file = scriptFile ?: return null
      val doc = FileDocumentManager.getInstance().getDocument(file) ?: return null
      return PsiDocumentManager.getInstance(project).getPsiFile(doc) as? KtFile
    }

  val scriptDoc: Document?
    get() = scriptPsi?.viewProvider?.document

  var data: TranscriptData? = null
    set(value) {
      if (value != field) {
        field = value
        fireTranscriptDataChanged()
      }
      with(UndoManager.getInstance(project)) {
        if (isRedoInProgress || isUndoInProgress) {
          return
        }
      }
      // Maybe, we don't need to update xml every time, because it is not used.
      updateXml()
    }
  var transcriptFile: VirtualFile? = null
    set(value) {
      setDependencies(field, value)
      field = value
    }
  val transcriptPsi: TranscriptPsiFile?
    get() {
      val file = transcriptFile ?: return null
      val doc = FileDocumentManager.getInstance().getDocument(file) ?: return null
      return PsiDocumentManager.getInstance(project).getPsiFile(doc) as? TranscriptPsiFile
    }


  private fun setDependencies(oldValue: VirtualFile?, value: VirtualFile?) {
    if (oldValue != null) {
      fileModelMap.remove(oldValue)
    }
    if (value != null) {
      fileModelMap[value] = this
    }
  }

  init {
    editionModel.addChangeListener(myEditionModelListener)
  }

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

  private fun EditionModel.EditionType.toWordDataState() = when (this) {
    CUT -> EXCLUDED
    MUTE -> MUTED
    NO_CHANGES -> PRESENTED
  }

  private fun synchronizeTranscriptWithEditionModel() {
    val words = data?.words ?: return
    val audio = audioDataModel ?: return
    for (word in words) {
      when (word.state) {
        EXCLUDED -> editionModel.cut(audio.msRangeToFrameRange(word.range))
        MUTED -> editionModel.mute(audio.msRangeToFrameRange(word.range))
        PRESENTED -> editionModel.undo(audio.msRangeToFrameRange(word.range))
      }
    }
  }

  interface Listener {
    fun onTranscriptDataChanged()
  }

  fun setAndReadXml(xmlFile: VirtualFile) {
    this.xmlFile = xmlFile
    val newData = xmlFile.inputStream.use { TranscriptData.createFrom(it) }
    data = newData.replaceWords(getWordReplacements(newData))
  }

  fun updateXml() {
    val nonNullData = data ?: return
    xmlFile.updateDoc { doc ->
      doc.setText(nonNullData.toXml())
    }
  }

  fun updateTranscript() {
    val nonNullData = data ?: return
//    transcriptFile?.setBinaryContent(nonNullData.text.toByteArray(charset = Charset.forName("UTF-8")))
    transcriptFile.updateDoc { doc ->
      with(PsiDocumentManager.getInstance(project)) {
        doPostponedOperationsAndUnblockDocument(doc)
        doc.setText(nonNullData.text)
        commitDocument(doc)
      }
    }
  }

  private fun VirtualFile?.updateDoc(action: (Document) -> Unit) {
    val file = this ?: return
    FileDocumentManager.getInstance().getDocument(file)?.let { doc ->
      ApplicationManager.getApplication().invokeAndWait {
        ApplicationManager.getApplication().runWriteAction {
          action(doc)
        }
      }
    }
  }

  fun addTranscriptDataListener(listener: Listener) {
    myListeners += listener
  }

  fun removeTranscriptDataListener(listener: Listener) {
    myListeners -= listener
  }

  private fun fireTranscriptDataChanged() {
    // Synchronize edition model with transcript data if it was changed in editor.
    // Also do not forger to reset coordinates cache.
    myTranscriptDataListener.onTranscriptDataChanged()
    for (x in myListeners) x.onTranscriptDataChanged()
  }

  fun replaceWords(replacements: List<Pair<Int, WordData>>) {
    if (replacements.isEmpty()) return
    data = data?.replaceWords(replacements)
  }

  fun renameWord(index: Int, text: String) {
    data = data?.renameWord(index, text)
  }

  fun changeRange(index: Int, newRange: IRange) {
    val word = data?.get(index) ?: return
    data = data?.replaceWords(listOf(index to word.copy(range = newRange)))
  }

  fun concatenateWords(indexRange: IRange) {
    data = data?.concatenateWords(indexRange)
  }

  fun bindWords(bindings: List<Pair<Int, RangeMarker?>>) {
    data = data?.bindWords(bindings)
  }

  fun excludeWords(indices: IntArray) {
    data = data?.excludeWords(indices)
  }

  fun excludeWord(index: Int) {
    data = data?.excludeWord(index)
  }

  fun showWords(indices: IntArray) {
    data = data?.showWords(indices)
  }

  fun muteWords(indices: IntArray) {
    data = data?.muteWords(indices)
  }

  fun createDefaultBinding() {
    val timedLines = TimeOffsetParser.parse(scriptPsi!!)
    val doc = scriptDoc!!
    val oldWords = data!!.words
    val newWords = mutableListOf<WordData>()
    intersect(oldWords, timedLines, { a, b -> a.range.intersects(b.time) }) { word, range ->
      val marker = if (range == null) {
        null
      } else {
        val startLine = range.first.lines.start
        val endLine = range.second.lines.end
        doc.createRangeMarker(doc.getLineStartOffset(startLine), doc.getLineEndOffset(endLine))
      }
      newWords.add(word.copy(bindStatements = marker))
    }
    newWords.forEach(::println)
    data!!.replaceWords(newWords.mapIndexed { index, x -> index to x })
  }

  override fun dispose() {
    myListeners.clear()
    xmlFile?.let { fileModelMap.remove(it) }
    transcriptFile?.let { fileModelMap.remove(it) }
    data = null
    xmlFile = null
    transcriptFile = null
    audioDataModel = null
    scriptFile = null
  }

  companion object {
    private val LOG = logger<MultimediaModel>()
    private val fileModelMap = ContainerUtil.newConcurrentMap<VirtualFile, MultimediaModel>()

    fun getOrCreate(project: Project, xmlFile: VirtualFile): MultimediaModel {
      return fileModelMap[xmlFile] ?: MultimediaModel(project)
    }

    fun get(virtualFile: VirtualFile): MultimediaModel? {
      return fileModelMap[virtualFile]
    }
  }
}
