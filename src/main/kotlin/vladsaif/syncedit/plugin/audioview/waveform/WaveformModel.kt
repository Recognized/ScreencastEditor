package vladsaif.syncedit.plugin.audioview.waveform

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Ref
import vladsaif.syncedit.plugin.*
import vladsaif.syncedit.plugin.audioview.waveform.impl.DefaultChangeNotifier
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.event.ChangeListener
import kotlin.math.max
import kotlin.math.min

class WaveformModel(val multimediaModel: MultimediaModel) : ChangeNotifier by DefaultChangeNotifier() {
    private val audioDataProvider
        get() = multimediaModel.audioDataModel
    /**
     * Waveform presented using sliding window and this is the visible part of it.
     */
    private val visibleRange
        get() = ClosedIntRange(_firstVisibleChunk, _firstVisibleChunk + _visibleChunks - 1)
    /**
     * Coordinates are expected to be called very frequently and most of the time they do not change.
     * So it worth to cache it, because it requires some possibly heavy calculations.
     */
    private val coordinates: List<ClosedIntRange>
        get() = if (coordinatesCacheCoherent) coordinatesCache
        else {
            val xx = multimediaModel.data?.words?.map { getCoordinates(it) }
            if (xx != null) {
                coordinatesCacheCoherent = true
                coordinatesCache = xx
            }
            xx ?: listOf()
        }
    @Volatile
    private var _audioData = listOf(AveragedSampleData())
    private var currentTask: Future<*>? = null
    private var currentTaskIsActive: AtomicBoolean? = null
    private var coordinatesCacheCoherent = false
    private var coordinatesCache = listOf<ClosedIntRange>()
    private var _visibleChunks = 4000
    private var _firstVisibleChunk = 0
    private var needInitialLoad = true
    private var lastLoadedVisibleRange: ClosedIntRange? = null
    private var _maxChunks = _visibleChunks
    private var broken = AtomicBoolean(false)
    @Volatile
    var playFramePosition = -1L
    val isBroken
        get() = broken.get()
    val maxChunks
        get() = _maxChunks
    val firstVisibleChunk
        get() = _firstVisibleChunk
    val audioData: List<AveragedSampleData>
        get() = _audioData.also {
            if (needInitialLoad) {
                loadData(_maxChunks, drawRange) {
                    fireStateChanged()
                }
                needInitialLoad = false
            }
        }
    val drawRange
        get() = ClosedIntRange(max(_firstVisibleChunk - _visibleChunks * 3, 0),
                min(_firstVisibleChunk + _visibleChunks * 3, _maxChunks - 1))
    val editionModel: EditionModel
        get() = multimediaModel.editionModel

    fun setRangeProperties(
            maxChunks: Int = _maxChunks,
            firstVisibleChunk: Int = _firstVisibleChunk,
            visibleChunks: Int = _visibleChunks
    ) {
        val model = audioDataProvider ?: return
        _maxChunks = minOf(max(maxChunks, (model.totalFrames / maxSamplesPerChunk).floorToInt()),
                (model.totalFrames / minSamplesPerChunk).floorToInt(),
                Int.MAX_VALUE / minSamplesPerChunk)
        _visibleChunks = max(min(visibleChunks, _maxChunks), 1)
        _firstVisibleChunk = max(min(firstVisibleChunk, _maxChunks - _visibleChunks - 1), 0)
    }

    init {
        multimediaModel.editionModel.addChangeListener(ChangeListener {
            fireStateChanged()
        })
        multimediaModel.addTranscriptDataListener(object : MultimediaModel.Listener {
            override fun onTranscriptDataChanged() {
                coordinatesCacheCoherent = false
                fireStateChanged()
            }
        })
    }

    fun getChunk(frame: Long) = audioDataProvider?.getChunk(_maxChunks, frame) ?: 0

    fun chunkRangeToFrameRange(chunkRange: ClosedIntRange): ClosedLongRange {
        val model = audioDataProvider ?: return ClosedLongRange.EMPTY_RANGE
        return ClosedLongRange(model.getStartFrame(_maxChunks, chunkRange.start),
                model.getStartFrame(_maxChunks, chunkRange.end + 1) - 1)
    }

    fun frameRangeToChunkRange(frameRange: ClosedLongRange): ClosedIntRange {
        val model = audioDataProvider ?: return ClosedIntRange.EMPTY_RANGE
        return ClosedIntRange(model.getChunk(_maxChunks, frameRange.start),
                model.getChunk(_maxChunks, frameRange.end))
    }

    fun getCoordinates(word: WordData): ClosedIntRange {
        val model = audioDataProvider ?: return ClosedIntRange.EMPTY_RANGE
        val left = model.getChunk(_maxChunks, (word.range.start / model.millisecondsPerFrame).toLong())
        val right = model.getChunk(_maxChunks, (word.range.end / model.millisecondsPerFrame).toLong())
        return ClosedIntRange(left, right)
    }

    fun getEnclosingWord(coordinate: Int): WordData? {
        val index = coordinates.binarySearch(ClosedIntRange(coordinate, coordinate), ClosedIntRange.INTERSECTS_CMP)
        return if (index < 0) null
        else multimediaModel.data?.words?.get(index)
    }

    /**
     * Start loading data in background for current position of sliding window.
     */
    fun updateData() {
        if (lastLoadedVisibleRange?.intersects(visibleRange) != true || visibleRange.length != lastLoadedVisibleRange?.length) {
            lastLoadedVisibleRange = visibleRange
            loadData(_maxChunks, drawRange) {
                fireStateChanged()
            }
        }
    }

    private fun loadData(maxChunks: Int, drawRange: ClosedIntRange, callback: () -> Unit) {
        if (broken.get()) return
        val model = audioDataProvider ?: return
        currentTaskIsActive?.set(false)
        currentTask?.cancel(true)
        try {
            currentTask?.get()
        } catch (_: CancellationException) {
        }
        val taskIsActive = AtomicBoolean(true)
        currentTaskIsActive = taskIsActive
        currentTask = ApplicationManager.getApplication().executeOnPooledThread {
            try {
                _audioData = model.getAveragedSampleData(maxChunks, drawRange, taskIsActive)
            } catch (ex: IOException) {
                if (broken.compareAndSet(false, true)) {
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

    fun scale(factor: Float, position: Int = _firstVisibleChunk + _visibleChunks / 2, callback: () -> Unit) {
        val centerPosition = position / maxChunks.toFloat()
        val currentMaxChunks = maxChunks
        val currentFirstVisible = _firstVisibleChunk
        val currentVisible = _visibleChunks
        setRangeProperties(maxChunks = (maxChunks * factor).toLong().floorToInt(),
                visibleChunks = (_visibleChunks * factor).toLong().floorToInt())
        val newMaxChunks = maxChunks
        val newCenterPoint = (newMaxChunks * centerPosition).toInt()
        val newVisibleRange = visibleRange
        val newValue = max(newCenterPoint - _visibleChunks / 2, 0)
        setRangeProperties(firstVisibleChunk = newValue)
        val newFirstVisible = _firstVisibleChunk
        val newVisibleChunks = _visibleChunks
        val newDrawRange = drawRange
        setRangeProperties(maxChunks = currentMaxChunks, firstVisibleChunk = currentFirstVisible, visibleChunks = currentVisible)
        loadData(newMaxChunks, newDrawRange) {
            resetCache()
            setRangeProperties(maxChunks = newMaxChunks, firstVisibleChunk = newFirstVisible, visibleChunks = newVisibleChunks)
            lastLoadedVisibleRange = newVisibleRange
            callback()
        }
    }

    private fun resetCache() {
        coordinatesCacheCoherent = false
        lastLoadedVisibleRange = null
    }

    fun getContainingWordRange(coordinate: Int) =
            coordinates.find { it.contains(coordinate) } ?: ClosedIntRange.EMPTY_RANGE

    fun getCoveredRange(extent: ClosedIntRange): ClosedIntRange {
        val coordinates = coordinates
        val left = coordinates.find { it.end >= extent.start }?.start ?: return ClosedIntRange.EMPTY_RANGE
        val right = coordinates.findLast { it.start <= extent.end }?.end ?: return ClosedIntRange.EMPTY_RANGE
        return ClosedIntRange(left, right)
    }

    companion object {
        private const val maxSamplesPerChunk = 100000
        private const val minSamplesPerChunk = 20
    }
}