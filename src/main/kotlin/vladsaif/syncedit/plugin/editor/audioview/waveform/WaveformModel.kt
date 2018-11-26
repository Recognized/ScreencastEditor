package vladsaif.syncedit.plugin.editor.audioview.waveform

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.ui.JBUI
import vladsaif.syncedit.plugin.editor.audioview.waveform.impl.DefaultChangeNotifier
import vladsaif.syncedit.plugin.model.ScreencastFile
import vladsaif.syncedit.plugin.model.WordData
import vladsaif.syncedit.plugin.sound.EditionModelView
import vladsaif.syncedit.plugin.util.*
import vladsaif.syncedit.plugin.util.Cache.Companion.cache
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.event.ChangeListener

class WaveformModel(
  val screencast: ScreencastFile,
  val audioDataModel: AudioDataModel
) : ChangeNotifier by DefaultChangeNotifier(), Disposable {

  private val myWordView = cache { calculateWordView() }
  @Volatile
  private var myAudioData = listOf(AveragedSampleData.ZERO)
  private var myCurrentTask: LoadTask? = null
  private var myNeedInitialLoad = true
  private var myLastLoadedVisibleRange: IntRange? = null
  private var myLastLoadedFramesPerPixel: Long = -1L
  private var myIsBroken = AtomicBoolean(false)
  private val myTranscriptListener: () -> Unit
  private val myEditionModelListener: () -> Unit = {
    fireStateChanged()
  }
  val pixelOffset: Int
    get() = screencast.coordinator.toPixel(audioDataModel.offsetFrames)
  var playFramePosition = -1L
    set(value) {
      field = value
      fireStateChanged()
    }
  val audioData: List<AveragedSampleData>
    get() = myAudioData.also {
      if (myNeedInitialLoad) {
        loadData()
        myNeedInitialLoad = false
      }
    }
  val drawRange = cache {
    val range = screencast.coordinator.visibleRange.mapInt { it.mulScale() }.shift(-pixelOffset)
    val length = range.length * 3
    val frameRange = screencast.coordinator.toFrameRange(range.start - length..range.endInclusive + length)
    screencast.coordinator.toPixelRange(editionModel.overlay(frameRange))
  }
  val editionModel: EditionModelView get() = screencast.editionModel
  val wordsView: List<WordView> get() = myWordView.get()

  private fun calculateWordView(): List<WordView> {
    val list = mutableListOf<WordView>()
    val words = screencast.data?.words ?: return listOf()
    for (word in words) {
      val wordFrameRange = screencast.coordinator.toFrameRange(word.range, TimeUnit.MILLISECONDS)
      list.add(WordView(word, screencast.coordinator.toPixelRange(editionModel.impose(wordFrameRange))))
    }
    return list
  }

  init {
    screencast.coordinator.addChangeListener(ChangeListener {
      drawRange.resetCache()
      loadData()
    })
    screencast.addEditionListener(myEditionModelListener)
    myTranscriptListener = {
      myWordView.resetCache()
      fireStateChanged()
    }
    screencast.addTranscriptListener(myTranscriptListener)
  }

  data class WordView(val word: WordData, val pixelRange: IntRange) {
    val leftBorder = IntRange(pixelRange.start - JBUI.scale(MAGNET_RANGE), pixelRange.start + JBUI.scale(MAGNET_RANGE))
    val rightBorder = IntRange(pixelRange.end - JBUI.scale(MAGNET_RANGE), pixelRange.end + JBUI.scale(MAGNET_RANGE))
  }

  fun getEnclosingWord(coordinate: Int): WordData? {
    val index = myWordView.get().map { it.pixelRange }.binarySearch(coordinate..coordinate, IntRange.INTERSECTS_CMP)
    return if (index < 0) null
    else screencast.data?.words?.get(index)
  }

  override fun dispose() {
    LOG.info("Disposed: waveform model of ${this.screencast}")
    screencast.removeTranscriptListener(myTranscriptListener)
    screencast.removeEditionListener(myEditionModelListener)
  }

  private fun loadData() {
    if (myLastLoadedVisibleRange?.intersects(screencast.coordinator.visibleRange) != true
      || screencast.coordinator.visibleRange.length != myLastLoadedVisibleRange?.length
      || screencast.coordinator.framesPerPixel != myLastLoadedFramesPerPixel
    ) {
      myLastLoadedVisibleRange = screencast.coordinator.visibleRange
      myLastLoadedFramesPerPixel = screencast.coordinator.framesPerPixel
      if (myIsBroken.get()) {
        return
      }
      myCurrentTask?.isActive = false
      LoadTask(screencast.coordinator.framesPerPixel.toInt(), drawRange.get()).let {
        myCurrentTask = it
        ApplicationManager.getApplication().executeOnPooledThread(it)
      }
    }
  }

  fun resetCache() {
    drawRange.resetCache()
    myWordView.resetCache()
    myLastLoadedVisibleRange = null
  }

  fun getContainingWordRange(coordinate: Int) =
    myWordView.get().map { it.pixelRange }.find { it.contains(coordinate) } ?: IntRange.EMPTY

  fun getCoveredRange(extent: IntRange): IntRange {
    val coordinates = myWordView.get().map { it.pixelRange }
    val left = coordinates.find { it.end >= extent.start }?.start ?: return IntRange.EMPTY
    val right = coordinates.findLast { it.start <= extent.end }?.end ?: return IntRange.EMPTY
    return IntRange(left, right)
  }

  fun fixWaveformDelta(delta: Int) {
    screencast.performModification {
      with(screencast.coordinator) {
        val deltaFrame = toFrame(delta.mulScale())
        audioDataModel.offsetFrames += deltaFrame
      }
    }
    resetCache()
  }


  private inner class LoadTask(
    private val framesPerChunk: Int,
    private val drawRange: IntRange
  ) : Runnable {
    private var start = 0L
    private val myIsActive = AtomicBoolean(true)
    var isActive = true
      set(value) {
        myIsActive.compareAndSet(true, false)
        field = value
      }

    override fun run() {
      try {
        start = System.currentTimeMillis()
        val computedResult = audioDataModel.getAveragedSampleData(framesPerChunk, drawRange) { isActive }
        if (myIsActive.compareAndSet(true, false)) {
          myAudioData = computedResult
        }
      } catch (ex: IOException) {
        if (myIsBroken.compareAndSet(false, true)) {
          showNotification("I/O error occurred during reading ${screencast.name} audio file. Try reopen file.")
        }
      } catch (ex: CancellationException) {
        return
      }
      ApplicationManager.getApplication().invokeLater {
        fireStateChanged()
      }
    }
  }


  companion object {
    private const val MAGNET_RANGE = 10
    private val LOG = logger<WaveformModel>()
  }
}