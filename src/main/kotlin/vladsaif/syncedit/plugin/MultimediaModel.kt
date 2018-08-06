package vladsaif.syncedit.plugin

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import vladsaif.syncedit.plugin.WordData.State.*
import vladsaif.syncedit.plugin.audioview.waveform.AudioDataModel
import vladsaif.syncedit.plugin.audioview.waveform.EditionModel
import vladsaif.syncedit.plugin.audioview.waveform.EditionModel.EditionType.*
import vladsaif.syncedit.plugin.audioview.waveform.impl.DefaultEditionModel
import vladsaif.syncedit.plugin.audioview.waveform.impl.SimpleAudioModel
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
      setDependencies(field, value)
      field = value
    }
  var audioFile: VirtualFile? = null
    set(value) {
      setDependencies(field, value)
      field = value
      if (value != null) {
        audioDataModel = SimpleAudioModel(File(value.path).toPath())
      }
    }
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
    // Synchronize edition model with transcript data if it was changed in editor.
    // Also do not forger to reset coordinates cache.
    addTranscriptDataListener(myTranscriptDataListener)
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
    val xml = xmlFile ?: return
    val nonNullData = data ?: return
    FileDocumentManager.getInstance().getDocument(xml)?.let { doc ->
      ApplicationManager.getApplication().runWriteAction {
        doc.setText(nonNullData.toXml())
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
    for (x in myListeners) x.onTranscriptDataChanged()
  }

  private fun replaceWords(replacements: List<Pair<Int, WordData>>) {
    if (replacements.isEmpty()) return
    data = data?.replaceWords(replacements)
  }

  fun renameWord(index: Int, text: String) {
    data = data?.renameWord(index, text)
  }

  fun concatenateWords(indexRange: ClosedIntRange) {
    data = data?.concatenateWords(indexRange)
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

  override fun dispose() {
    myListeners.clear()
    xmlFile?.let { fileModelMap.remove(it) }
    transcriptFile?.let { fileModelMap.remove(it) }
    data = null
    xmlFile = null
    transcriptFile = null
    audioDataModel = null
  }

  companion object {
    private val LOG = logger<MultimediaModel>()
    private val fileModelMap = mutableMapOf<VirtualFile, MultimediaModel>()

    fun getOrCreate(project: Project, xmlFile: VirtualFile): MultimediaModel {
      return fileModelMap[xmlFile] ?: MultimediaModel(project)
    }

    fun get(virtualFile: VirtualFile): MultimediaModel? {
      return fileModelMap[virtualFile]
    }
  }
}
