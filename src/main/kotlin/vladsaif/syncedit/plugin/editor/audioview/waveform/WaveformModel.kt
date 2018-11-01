package vladsaif.syncedit.plugin.editor.audioview.waveform

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.ui.JBUI
import vladsaif.syncedit.plugin.editor.audioview.waveform.impl.DefaultChangeNotifier
import vladsaif.syncedit.plugin.editor.scriptview.AudioCoordinator
import vladsaif.syncedit.plugin.model.ScreencastFile
import vladsaif.syncedit.plugin.model.WordData
import vladsaif.syncedit.plugin.sound.EditionModel
import vladsaif.syncedit.plugin.util.*
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.swing.event.ChangeListener
import kotlin.math.max
import kotlin.math.min

class WaveformModel(val screencast: ScreencastFile) : ChangeNotifier by DefaultChangeNotifier(), Disposable {
  val audioDataModel = screencast.audioDataModel!!
  /**
   * Waveform presented using sliding window and this is the visible part of it.
   */
  private val myVisibleRange
    get() = IntRange(myFirstVisibleChunk, myFirstVisibleChunk + myVisibleChunks - 1)
  /**
   * Coordinates are expected to be called very frequently and most of the time they do not change.
   * So it worth to cache it, because it requires some possibly heavy calculations.
   */
  private val myCoordinates: List<IntRange>
    get() = if (myCoordinatesCacheCoherent) myCoordinatesCache
    else {
      val xx = screencast.data?.words?.map { getCoordinates(it) }
      if (xx != null) {
        myCoordinatesCacheCoherent = true
        myCoordinatesCache = xx
        myWordBorders = calculateWordBorders()
      }
      xx ?: listOf()
    }
  @Volatile
  private var myAudioData = listOf(AveragedSampleData())
  private var myCurrentTask: LoadTask? = null
  private var myCoordinatesCacheCoherent = false
  private var myCoordinatesCache = listOf<IntRange>()
  private var myVisibleChunks = 4000
  private var myFirstVisibleChunk = 0
  private var myNeedInitialLoad = true
  private var myLastLoadedVisibleRange: IntRange? = null
  private var myWordBorders: List<IntRange> = listOf()
  private var myIsBroken = AtomicBoolean(false)
  private val myTranscriptListener: () -> Unit
  private val myEditionModelListener: ChangeListener
  var playFramePosition: AtomicLong = AtomicLong(-1L)
  // TODO make use of it
  val isBroken
    get() = myIsBroken.get()
  val coordinator = AudioCoordinator(audioDataModel)
  val maxChunks
    get() = coordinator.maxPixels
  val firstVisibleChunk
    get() = myFirstVisibleChunk
  val audioData: List<AveragedSampleData>
    get() = myAudioData.also {
      if (myNeedInitialLoad) {
        loadData(maxChunks, drawRange) {
          fireStateChanged()
        }
        myNeedInitialLoad = false
      }
    }
  val drawRange: IntRange
    get() = IntRange(
      max(myFirstVisibleChunk - myVisibleChunks * 3, 0),
      min(myFirstVisibleChunk + myVisibleChunks * 3, maxChunks - 1)
    )
  val editionModel: EditionModel
    get() = screencast.editionModel
  val wordBorders: List<IntRange>
    get() {
      if (!myCoordinatesCacheCoherent) {
        myCoordinates
      }
      return myWordBorders
    }
  val wordCoordinates: List<IntRange>
    get() = myCoordinates

  private fun calculateWordBorders(): List<IntRange> {
    val list = mutableListOf<IntRange>()
    for (x in myCoordinatesCache) {
      list.add(IntRange(x.start - JBUI.scale(MAGNET_RANGE), x.start + JBUI.scale(MAGNET_RANGE)))
      list.add(IntRange(x.end - JBUI.scale(MAGNET_RANGE), x.end + JBUI.scale(MAGNET_RANGE)))
    }
    return list
  }

  fun setRangeProperties(
    maxChunks: Int = this.maxChunks,
    firstVisibleChunk: Int = myFirstVisibleChunk,
    visibleChunks: Int = myVisibleChunks
  ) {
    val model = audioDataModel
    coordinator.maxPixels = minOf(
      max(maxChunks, (model.totalFrames / MAX_SAMPLES_PER_CHUNK).floorToInt()),
      (model.totalFrames / MIN_SAMPLES_PER_CHUNK).floorToInt(),
      Int.MAX_VALUE / MIN_SAMPLES_PER_CHUNK
    )
    myVisibleChunks = max(min(visibleChunks, this.maxChunks), 1)
    myFirstVisibleChunk = max(min(firstVisibleChunk, this.maxChunks - myVisibleChunks - 1), 0)
  }

  init {
    myEditionModelListener = ChangeListener {
      fireStateChanged()
    }
    screencast.editionModel.addChangeListener(myEditionModelListener)
    myTranscriptListener = {
      myCoordinatesCacheCoherent = false
      fireStateChanged()
    }
    screencast.addTranscriptDataListener(myTranscriptListener)
  }

  fun getCoordinates(word: WordData): IntRange {
    val left = coordinator.getPixel((word.range.start / audioDataModel.millisecondsPerFrame).toLong())
    val right = coordinator.getPixel((word.range.end / audioDataModel.millisecondsPerFrame).toLong())
    return IntRange(left, right)
  }

  fun getEnclosingWord(coordinate: Int): WordData? {
    val index = myCoordinates.binarySearch(IntRange(coordinate, coordinate), IntRange.INTERSECTS_CMP)
    return if (index < 0) null
    else screencast.data?.words?.get(index)
  }

  /**
   * Start loading data in background for current position of sliding window.
   */
  fun updateData() {
    if (myLastLoadedVisibleRange?.intersects(myVisibleRange) != true
      || myVisibleRange.length != myLastLoadedVisibleRange?.length
    ) {
      myLastLoadedVisibleRange = myVisibleRange
      loadData(maxChunks, drawRange) {
        fireStateChanged()
      }
    }
  }

  override fun dispose() {
    LOG.info("Disposed: waveform model of ${this.screencast}")
    screencast.removeTranscriptDataListener(myTranscriptListener)
    screencast.editionModel.removeChangeListener(myEditionModelListener)
  }

  inner class LoadTask(
    private val maxChunks: Int,
    private val drawRange: IntRange,
    private val callback: () -> Unit
  ) : Runnable {
    @Volatile
    var isActive = true
    private var start = 0L
    override fun run() {
      try {
        start = System.currentTimeMillis()
        val computedResult = audioDataModel.getAveragedSampleData(maxChunks, drawRange) { isActive }
        ApplicationManager.getApplication().invokeAndWait {
          if (isActive) {
            myAudioData = computedResult
          }
        }
      } catch (ex: IOException) {
        if (myIsBroken.compareAndSet(false, true)) {
          showNotification("I/O error occurred during reading ${screencast.name} audio file. Try reopen file.")
        }
      } catch (ex: CancellationException) {
        return
      }
      ApplicationManager.getApplication().invokeLater {
        callback()
      }
    }
  }


  private fun loadData(maxChunks: Int, drawRange: IntRange, callback: () -> Unit) {
    if (isBroken) return
    myCurrentTask?.isActive = false
    val newTask = LoadTask(maxChunks, drawRange, callback)
    myCurrentTask = newTask
    ApplicationManager.getApplication().executeOnPooledThread(newTask)
  }

  fun scale(
    factor: Float,
    position: Int = myFirstVisibleChunk + myVisibleChunks / 2,
    callback: () -> Unit
  ) {
    val centerPosition = position / maxChunks.toFloat()
    val currentMaxChunks = maxChunks
    val currentFirstVisible = myFirstVisibleChunk
    val currentVisible = myVisibleChunks
    setRangeProperties(
      maxChunks = (maxChunks * factor).toLong().floorToInt(),
      visibleChunks = (myVisibleChunks * factor).toLong().floorToInt()
    )
    val newMaxChunks = maxChunks
    val newCenterPoint = (newMaxChunks * centerPosition).toInt()
    val newVisibleRange = myVisibleRange
    val newValue = max(newCenterPoint - myVisibleChunks / 2, 0)
    setRangeProperties(firstVisibleChunk = newValue)
    val newFirstVisible = myFirstVisibleChunk
    val newVisibleChunks = myVisibleChunks
    val newDrawRange = drawRange
    setRangeProperties(
      maxChunks = currentMaxChunks,
      firstVisibleChunk = currentFirstVisible,
      visibleChunks = currentVisible
    )
    loadData(newMaxChunks, newDrawRange) {
      resetCache()
      setRangeProperties(
        maxChunks = newMaxChunks,
        firstVisibleChunk = newFirstVisible,
        visibleChunks = newVisibleChunks
      )
      myLastLoadedVisibleRange = newVisibleRange
      callback()
    }
  }

  private fun resetCache() {
    myCoordinatesCacheCoherent = false
    myLastLoadedVisibleRange = null
  }

  fun getContainingWordRange(coordinate: Int) =
    myCoordinates.find { it.contains(coordinate) } ?: IntRange.EMPTY

  fun getCoveredRange(extent: IntRange): IntRange {
    val coordinates = myCoordinates
    val left = coordinates.find { it.end >= extent.start }?.start ?: return IntRange.EMPTY
    val right = coordinates.findLast { it.start <= extent.end }?.end ?: return IntRange.EMPTY
    return IntRange(left, right)
  }

  companion object {
    private const val MAX_SAMPLES_PER_CHUNK = 100000
    private const val MIN_SAMPLES_PER_CHUNK = 20
    private const val MAGNET_RANGE = 10
    private val LOG = logger<WaveformModel>()
  }
}