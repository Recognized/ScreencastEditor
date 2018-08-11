package vladsaif.syncedit.plugin.audioview.waveform

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.ui.JBUI
import vladsaif.syncedit.plugin.*
import vladsaif.syncedit.plugin.audioview.waveform.impl.DefaultChangeNotifier
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.swing.event.ChangeListener
import kotlin.math.max
import kotlin.math.min

class WaveformModel(val multimediaModel: MultimediaModel) : ChangeNotifier by DefaultChangeNotifier() {
  private val myAudioDataProvider
    get() = multimediaModel.audioDataModel
  /**
   * Waveform presented using sliding window and this is the visible part of it.
   */
  private val myVisibleRange
    get() = ClosedIntRange(myFirstVisibleChunk, myFirstVisibleChunk + myVisibleChunks - 1)
  /**
   * Coordinates are expected to be called very frequently and most of the time they do not change.
   * So it worth to cache it, because it requires some possibly heavy calculations.
   */
  private val myCoordinates: List<ClosedIntRange>
    get() = if (myCoordinatesCacheCoherent) myCoordinatesCache
    else {
      val xx = multimediaModel.data?.words?.map { getCoordinates(it) }
      if (xx != null) {
        myCoordinatesCacheCoherent = true
        myCoordinatesCache = xx
        myWordBorders = calculateWordBorders()
      }
      xx ?: listOf()
    }
  @Volatile
  private var myAudioData = listOf(AveragedSampleData())
  private var myCurrentTask: Future<*>? = null
  private var myCurrentTaskIsActive: AtomicBoolean? = null
  private var myCoordinatesCacheCoherent = false
  private var myCoordinatesCache = listOf<ClosedIntRange>()
  private var myVisibleChunks = 4000
  private var myFirstVisibleChunk = 0
  private var myNeedInitialLoad = true
  private var myLastLoadedVisibleRange: ClosedIntRange? = null
  private var myMaxChunks = myVisibleChunks
  private var myIsBroken = AtomicBoolean(false)
  var playFramePosition: AtomicLong = AtomicLong(-1L)
  // TODO use it
  val isBroken
    get() = myIsBroken.get()
  val maxChunks
    get() = myMaxChunks
  val firstVisibleChunk
    get() = myFirstVisibleChunk
  val audioData: List<AveragedSampleData>
    get() = myAudioData.also {
      if (myNeedInitialLoad) {
        loadData(myMaxChunks, drawRange) {
          fireStateChanged()
        }
        myNeedInitialLoad = false
      }
    }
  val drawRange: ClosedIntRange
    get() = ClosedIntRange(max(myFirstVisibleChunk - myVisibleChunks * 3, 0),
        min(myFirstVisibleChunk + myVisibleChunks * 3, myMaxChunks - 1))
  val editionModel: EditionModel
    get() = multimediaModel.editionModel
  private var myWordBorders: List<ClosedIntRange> = listOf()
  val wordBorders: List<ClosedIntRange>
    get() {
      if (!myCoordinatesCacheCoherent) {
        myCoordinates
      }
      return myWordBorders
    }
  val wordCoordinates: List<ClosedIntRange>
    get() = myCoordinates

  private fun calculateWordBorders(): List<ClosedIntRange> {
    val list = mutableListOf<ClosedIntRange>()
    for (x in myCoordinatesCache) {
      list.add(ClosedIntRange(x.start - JBUI.scale(magnetRange), x.start + JBUI.scale(magnetRange)))
      list.add(ClosedIntRange(x.end - JBUI.scale(magnetRange), x.end + JBUI.scale(magnetRange)))
    }
    return list
  }

  fun setRangeProperties(
      maxChunks: Int = myMaxChunks,
      firstVisibleChunk: Int = myFirstVisibleChunk,
      visibleChunks: Int = myVisibleChunks
  ) {
    val model = myAudioDataProvider ?: return
    myMaxChunks = minOf(max(maxChunks, (model.totalFrames / maxSamplesPerChunk).floorToInt()),
        (model.totalFrames / minSamplesPerChunk).floorToInt(),
        Int.MAX_VALUE / minSamplesPerChunk)
    myVisibleChunks = max(min(visibleChunks, myMaxChunks), 1)
    myFirstVisibleChunk = max(min(firstVisibleChunk, myMaxChunks - myVisibleChunks - 1), 0)
  }

  init {
    multimediaModel.editionModel.addChangeListener(ChangeListener {
      fireStateChanged()
    })
    multimediaModel.addTranscriptDataListener(object : MultimediaModel.Listener {
      override fun onTranscriptDataChanged() {
        myCoordinatesCacheCoherent = false
        fireStateChanged()
      }
    })
  }

  fun getChunk(frame: Long) = myAudioDataProvider?.getChunk(myMaxChunks, frame) ?: 0

  fun chunkRangeToFrameRange(chunkRange: ClosedIntRange): ClosedLongRange {
    val model = myAudioDataProvider ?: return ClosedLongRange.EMPTY_RANGE
    return ClosedLongRange(model.getStartFrame(myMaxChunks, chunkRange.start),
        model.getStartFrame(myMaxChunks, chunkRange.end + 1) - 1)
  }

  fun frameRangeToChunkRange(frameRange: ClosedLongRange): ClosedIntRange {
    val model = myAudioDataProvider ?: return ClosedIntRange.EMPTY_RANGE
    return ClosedIntRange(model.getChunk(myMaxChunks, frameRange.start),
        model.getChunk(myMaxChunks, frameRange.end))
  }

  fun getCoordinates(word: WordData): ClosedIntRange {
    val model = myAudioDataProvider ?: return ClosedIntRange.EMPTY_RANGE
    val left = model.getChunk(myMaxChunks, (word.range.start / model.millisecondsPerFrame).toLong())
    val right = model.getChunk(myMaxChunks, (word.range.end / model.millisecondsPerFrame).toLong())
    return ClosedIntRange(left, right)
  }

  fun getEnclosingWord(coordinate: Int): WordData? {
    val index = myCoordinates.binarySearch(ClosedIntRange(coordinate, coordinate), ClosedIntRange.INTERSECTS_CMP)
    return if (index < 0) null
    else multimediaModel.data?.words?.get(index)
  }

  /**
   * Start loading data in background for current position of sliding window.
   */
  fun updateData() {
    if (myLastLoadedVisibleRange?.intersects(myVisibleRange) != true || myVisibleRange.length != myLastLoadedVisibleRange?.length) {
      myLastLoadedVisibleRange = myVisibleRange
      loadData(myMaxChunks, drawRange) {
        fireStateChanged()
      }
    }
  }

  private fun loadData(maxChunks: Int, drawRange: ClosedIntRange, callback: () -> Unit) {
    if (myIsBroken.get()) return
    val model = myAudioDataProvider ?: return
    myCurrentTaskIsActive?.set(false)
    myCurrentTask?.cancel(true)
    try {
      myCurrentTask?.get()
    } catch (_: CancellationException) {
    }
    val taskIsActive = AtomicBoolean(true)
    myCurrentTaskIsActive = taskIsActive
    myCurrentTask = ApplicationManager.getApplication().executeOnPooledThread {
      try {
        myAudioData = model.getAveragedSampleData(maxChunks, drawRange, taskIsActive)
      } catch (ex: IOException) {
        if (myIsBroken.compareAndSet(false, true)) {
          showNotification("I/O error occurred during reading ${multimediaModel.audioFile} audio file. Try reopen file.")
        }
      } catch (ex: CancellationException) {
        return@executeOnPooledThread
      }
      ApplicationManager.getApplication().invokeLater {
        callback()
      }
    }
  }

  fun scale(factor: Float, position: Int = myFirstVisibleChunk + myVisibleChunks / 2, callback: () -> Unit) {
    val centerPosition = position / maxChunks.toFloat()
    val currentMaxChunks = maxChunks
    val currentFirstVisible = myFirstVisibleChunk
    val currentVisible = myVisibleChunks
    setRangeProperties(maxChunks = (maxChunks * factor).toLong().floorToInt(),
        visibleChunks = (myVisibleChunks * factor).toLong().floorToInt())
    val newMaxChunks = maxChunks
    val newCenterPoint = (newMaxChunks * centerPosition).toInt()
    val newVisibleRange = myVisibleRange
    val newValue = max(newCenterPoint - myVisibleChunks / 2, 0)
    setRangeProperties(firstVisibleChunk = newValue)
    val newFirstVisible = myFirstVisibleChunk
    val newVisibleChunks = myVisibleChunks
    val newDrawRange = drawRange
    setRangeProperties(maxChunks = currentMaxChunks, firstVisibleChunk = currentFirstVisible, visibleChunks = currentVisible)
    loadData(newMaxChunks, newDrawRange) {
      resetCache()
      setRangeProperties(maxChunks = newMaxChunks, firstVisibleChunk = newFirstVisible, visibleChunks = newVisibleChunks)
      myLastLoadedVisibleRange = newVisibleRange
      callback()
    }
  }

  private fun resetCache() {
    myCoordinatesCacheCoherent = false
    myLastLoadedVisibleRange = null
  }

  fun getContainingWordRange(coordinate: Int) =
      myCoordinates.find { it.contains(coordinate) } ?: ClosedIntRange.EMPTY_RANGE

  fun getCoveredRange(extent: ClosedIntRange): ClosedIntRange {
    val coordinates = myCoordinates
    val left = coordinates.find { it.end >= extent.start }?.start ?: return ClosedIntRange.EMPTY_RANGE
    val right = coordinates.findLast { it.start <= extent.end }?.end ?: return ClosedIntRange.EMPTY_RANGE
    return ClosedIntRange(left, right)
  }

  companion object {
    private const val maxSamplesPerChunk = 100000
    private const val minSamplesPerChunk = 20
    private const val magnetRange = 10
  }
}