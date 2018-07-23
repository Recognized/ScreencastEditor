package vladsaif.syncedit.plugin.audioview.waveform

import com.intellij.openapi.application.ApplicationManager
import kotlinx.coroutines.experimental.*
import vladsaif.syncedit.plugin.ClosedIntRange
import vladsaif.syncedit.plugin.ClosedLongRange
import vladsaif.syncedit.plugin.Word
import vladsaif.syncedit.plugin.audioview.waveform.impl.BasicStatProvider
import vladsaif.syncedit.plugin.audioview.waveform.impl.DefaultChangeNotifier
import vladsaif.syncedit.plugin.audioview.waveform.impl.DefaultEditionModel
import vladsaif.syncedit.plugin.floorToInt
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

class WaveformModel(val file: Path) : ChangeNotifier by DefaultChangeNotifier() {
    private val audioDataProvider = BasicStatProvider(file)
    private val visibleRange
        get() = ClosedIntRange(_firstVisibleChunk, _firstVisibleChunk + _visibleChunks - 1)
    private val coordinates
        get() = if (coordinatesCacheCoherent) coordinatesCache
        else wordData.map { getCoordinates(it) }.also {
            coordinatesCache = it
            coordinatesCacheCoherent = true
        }
    @Volatile
    private var _audioData = listOf(AveragedSampleData())
    private var currentTask: Job? = null
    private var coordinatesCacheCoherent = true
    private var coordinatesCache = listOf<ClosedIntRange>()
    private var _visibleChunks = 4000
    private var _firstVisibleChunk = 0
    private var needInitialLoad = true
    private var lastLoadedVisibleRange: ClosedIntRange? = null
    private var _maxChunks = _visibleChunks
    private var broken = AtomicBoolean(false)
    val editionModel: EditionModel = DefaultEditionModel()
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
    var wordData = listOf<Word>()
        set(value) {
            field = value.sorted()
            coordinatesCacheCoherent = false
            fireStateChanged()
        }

    fun setRangeProperties(
            maxChunks: Int = this.maxChunks,
            firstVisibleChunk: Int = this._firstVisibleChunk,
            visibleChunks: Int = this._visibleChunks
    ) {
        this._maxChunks = minOf(max(maxChunks, (audioDataProvider.totalFrames / maxSamplesPerChunk).floorToInt()),
                (audioDataProvider.totalFrames / minSamplesPerChunk).floorToInt(),
                Int.MAX_VALUE / minSamplesPerChunk)
        this._visibleChunks = max(min(visibleChunks, this._maxChunks), 1)
        this._firstVisibleChunk = max(min(firstVisibleChunk, this.maxChunks - this._visibleChunks - 1), 0)
    }

    fun getChunk(frame: Long) = audioDataProvider.getChunk(_maxChunks, frame)

    fun chunkRangeToFrameRange(chunkRange: ClosedIntRange): ClosedLongRange {
        return ClosedLongRange(audioDataProvider.getStartFrame(_maxChunks, chunkRange.start),
                audioDataProvider.getStartFrame(_maxChunks, chunkRange.end + 1) - 1)
    }

    fun frameRangeToChunkRange(frameRange: ClosedLongRange): ClosedIntRange {
        return ClosedIntRange(audioDataProvider.getChunk(_maxChunks, frameRange.start),
                audioDataProvider.getChunk(_maxChunks, frameRange.end))
    }

    fun getCoordinates(word: Word) = ClosedIntRange(getXCoordinate(word.startMillisecond), getXCoordinate(word.endMilliseconds))

    private fun getXCoordinate(x: Double) =
            audioDataProvider.getChunk(_maxChunks, (x / audioDataProvider.millisecondsPerFrame).toLong())

    fun getEnclosingWord(coordinate: Int): Word? {
        val index = coordinates.binarySearch(ClosedIntRange(coordinate, coordinate), ClosedIntRange.INTERSECTS_CMP)
        return if (index < 0) null
        else wordData[index]
    }

    fun updateData() {
        if (lastLoadedVisibleRange?.intersects(visibleRange) != true || visibleRange.length != lastLoadedVisibleRange?.length) {
            lastLoadedVisibleRange = visibleRange
            loadData(_maxChunks, drawRange) {
                fireStateChanged()
            }
        }
    }

    private fun loadData(maxChunks: Int, drawRange: ClosedIntRange, callback: () -> Unit) = runBlocking {
        if (broken.get()) return@runBlocking
        currentTask?.cancelAndJoin()
        val job = Job()
        currentTask = job
        launch(CommonPool, parent = job) {
            try {
                _audioData = audioDataProvider.getAveragedSampleData(maxChunks, drawRange, job)
            } catch (ex: IOException) {
                if (broken.compareAndSet(false, true)) {
                    showNotification("I/O error occurred during reading $file audio file. Try reopen file.")
                }
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