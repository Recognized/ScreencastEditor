package vladsaif.syncedit.plugin.audioview.waveform

import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.ui.JBUI
import vladsaif.syncedit.plugin.audioview.waveform.impl.DefaultChangeNotifier
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

class WaveformModel(val screencast: ScreencastFile) : ChangeNotifier by DefaultChangeNotifier() {
  private val myAudioDataProvider
    get() = screencast.audioDataModel
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
  val drawRange: IntRange
    get() = IntRange(max(myFirstVisibleChunk - myVisibleChunks * 3, 0),
        min(myFirstVisibleChunk + myVisibleChunks * 3, myMaxChunks - 1))
  val editionModel: EditionModel
    get() = screencast.editionModel
  private var myWordBorders: List<IntRange> = listOf()
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
      list.add(IntRange(x.start - JBUI.scale(magnetRange), x.start + JBUI.scale(magnetRange)))
      list.add(IntRange(x.end - JBUI.scale(magnetRange), x.end + JBUI.scale(magnetRange)))
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
    screencast.editionModel.addChangeListener(ChangeListener {
      fireStateChanged()
    })
    screencast.addTranscriptDataListener(object : ScreencastFile.Listener {
      override fun onTranscriptDataChanged() {
        myCoordinatesCacheCoherent = false
        fireStateChanged()
      }
    })
  }

  fun getChunk(frame: Long) = myAudioDataProvider?.getChunk(myMaxChunks, frame) ?: 0

  fun chunkRangeToFrameRange(chunkRange: IntRange): LongRange {
    val model = myAudioDataProvider ?: return LongRange.EMPTY
    return LongRange(model.getStartFrame(myMaxChunks, chunkRange.start),
        model.getStartFrame(myMaxChunks, chunkRange.end + 1) - 1)
  }

  fun frameRangeToChunkRange(frameRange: LongRange): IntRange {
    val model = myAudioDataProvider ?: return IntRange.EMPTY
    return IntRange(model.getChunk(myMaxChunks, frameRange.start),
        model.getChunk(myMaxChunks, frameRange.end))
  }

  fun getCoordinates(word: WordData): IntRange {
    val model = myAudioDataProvider ?: return IntRange.EMPTY
    val left = model.getChunk(myMaxChunks, (word.range.start / model.millisecondsPerFrame).toLong())
    val right = model.getChunk(myMaxChunks, (word.range.end / model.millisecondsPerFrame).toLong())
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
        || myVisibleRange.length != myLastLoadedVisibleRange?.length) {
      myLastLoadedVisibleRange = myVisibleRange
      loadData(myMaxChunks, drawRange) {
        fireStateChanged()
      }
    }
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
        val model = myAudioDataProvider ?: return
        val computedResult = model.getAveragedSampleData(maxChunks, drawRange) { isActive }
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
      myCoordinates.find { it.contains(coordinate) } ?: IntRange.EMPTY

  fun getCoveredRange(extent: IntRange): IntRange {
    val coordinates = myCoordinates
    val left = coordinates.find { it.end >= extent.start }?.start ?: return IntRange.EMPTY
    val right = coordinates.findLast { it.start <= extent.end }?.end ?: return IntRange.EMPTY
    return IntRange(left, right)
  }

  companion object {
    private const val maxSamplesPerChunk = 100000
    private const val minSamplesPerChunk = 20
    private const val magnetRange = 10
  }
}