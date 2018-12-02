package vladsaif.syncedit.plugin.editor.audioview.waveform

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.ui.JBUI
import vladsaif.syncedit.plugin.actions.showNotification
import vladsaif.syncedit.plugin.editor.audioview.waveform.impl.DefaultChangeNotifier
import vladsaif.syncedit.plugin.model.Screencast
import vladsaif.syncedit.plugin.model.WordData
import vladsaif.syncedit.plugin.util.*
import vladsaif.syncedit.plugin.util.Cache.Companion.cache
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.event.ChangeListener

class WaveformModel(
  val screencast: Screencast,
  val audio: Screencast.Audio
) : ChangeNotifier by DefaultChangeNotifier(), Disposable {

  private inline val myCoordinator get() = screencast.coordinator
  private inline val myEditionsModel get() = audio.editionsModel
  private inline val myAudioModel get() = audio.model
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
    myAudioPixels.resetCache()
    fireStateChanged()
  }
  private val myDrawRange = cache {
    val range = myCoordinator.visibleRange.mapInt { it.mulScale() }.shift(-pixelOffset)
    val length = range.length * 3
    val frameRange = myCoordinator.toFrameRange(range.start - length..range.endInclusive + length)
    myCoordinator.toPixelRange(myEditionsModel.overlay(frameRange))
  }
  private val myAudioPixels = cache {
    0..myCoordinator.toPixel(audio.editionsModel.impose(audio.model.totalFrames - 1))
  }
  val pixelOffset: Int
    get() = myCoordinator.toPixel(myAudioModel.offsetFrames)
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
  val drawRange = myDrawRange.get()
  val audioPixels = myAudioPixels.get()
  val wordsView: List<WordView> get() = myWordView.get()

  private fun calculateWordView(): List<WordView> {
    val list = mutableListOf<WordView>()
    val words = audio.data?.words ?: return listOf()
    for (word in words) {
      val wordFrameRange = myCoordinator.toFrameRange(word.range, TimeUnit.MILLISECONDS)
      list.add(WordView(word, myCoordinator.toPixelRange(myEditionsModel.impose(wordFrameRange))))
    }
    return list
  }

  init {
    myCoordinator.addChangeListener(ChangeListener {
      myDrawRange.resetCache()
      myAudioPixels.resetCache()
      loadData()
    })
    screencast.addEditionListener(audio, myEditionModelListener)
    myTranscriptListener = {
      myWordView.resetCache()
      fireStateChanged()
    }
    screencast.addTranscriptListener(audio, myTranscriptListener)
  }

  data class WordView(val word: WordData, val pixelRange: IntRange) {
    val leftBorder = IntRange(pixelRange.start - JBUI.scale(MAGNET_RANGE), pixelRange.start + JBUI.scale(MAGNET_RANGE))
    val rightBorder = IntRange(pixelRange.end - JBUI.scale(MAGNET_RANGE), pixelRange.end + JBUI.scale(MAGNET_RANGE))
  }

  fun getEnclosingWord(coordinate: Int): WordData? {
    val index = myWordView.get().map { it.pixelRange }.binarySearch(coordinate..coordinate, IntRange.INTERSECTS_CMP)
    return if (index < 0) null
    else audio.data?.words?.get(index)
  }

  override fun dispose() {
    LOG.info("Disposed: waveform model of ${this.screencast}")
    screencast.removeTranscriptListener(audio, myTranscriptListener)
    screencast.removeEditionListener(audio, myEditionModelListener)
  }

  private fun loadData() {
    if (myLastLoadedVisibleRange?.intersects(myCoordinator.visibleRange) != true
      || myCoordinator.visibleRange.length != myLastLoadedVisibleRange?.length
      || myCoordinator.framesPerPixel != myLastLoadedFramesPerPixel
    ) {
      myLastLoadedVisibleRange = myCoordinator.visibleRange
      myLastLoadedFramesPerPixel = myCoordinator.framesPerPixel
      if (myIsBroken.get()) {
        return
      }
      myCurrentTask?.isActive = false
      LoadTask(myCoordinator.framesPerPixel.toInt(), drawRange).let {
        myCurrentTask = it
        ApplicationManager.getApplication().executeOnPooledThread(it)
      }
    }
  }

  fun resetCache() {
    myDrawRange.resetCache()
    myAudioPixels.resetCache()
    myWordView.resetCache()
    myLastLoadedVisibleRange = null
  }

  fun getContainingWordRange(coordinate: Int) =
    myWordView.get().map { it.pixelRange }.find { it.contains(coordinate) } ?: IntRange.EMPTY

  fun getCoveredRange(extent: IntRange): IntRange {
    val coordinates = myWordView.get().map { it.pixelRange }
    val left = coordinates.find { it.end >= extent.start }?.start ?: return IntRange.EMPTY
    val right = coordinates.findLast { it.start <= extent.end }?.end ?: return IntRange.EMPTY
    return left..right
  }

  fun fixWaveformDelta(delta: Int) {
    screencast.performModification {
      with(myCoordinator) {
        val deltaFrame = toFrame(delta.mulScale())
        getEditable(audio).model.offsetFrames += deltaFrame
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
        val computedResult = myAudioModel.getAveragedSampleData(framesPerChunk, drawRange) { isActive }
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