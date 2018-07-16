package vladsaif.syncedit.plugin.audioview.waveform

import com.intellij.openapi.application.ApplicationManager
import org.picocontainer.Disposable
import vladsaif.syncedit.plugin.ClosedIntRange
import vladsaif.syncedit.plugin.ClosedLongRange
import vladsaif.syncedit.plugin.audioview.waveform.Player.PlayState.*
import vladsaif.syncedit.plugin.showNotification
import java.awt.Dimension
import java.io.IOException
import javax.swing.JScrollPane
import javax.swing.event.ChangeEvent

class WaveformController(private val waveform: JWaveform) : Disposable {
    private var mScrollPane: JScrollPane? = null
    private var player: Player? = null
    private var ignoreBarChanges = false
    @Volatile
    private var blockScaling = false
    @Volatile
    private var _playState = STOP
    val playState
        get() = _playState

    fun installZoom(scrollPane: JScrollPane) {
        mScrollPane = scrollPane
        val brm = scrollPane.horizontalScrollBar.model
        scrollPane.horizontalScrollBar.addAdjustmentListener {
            if (ignoreBarChanges) return@addAdjustmentListener
            waveform.model.setRangeProperties(visibleChunks = brm.extent, firstVisibleChunk = brm.value, maxChunks = brm.maximum)
            waveform.model.updateData()
        }
    }

    fun zoomIn() {
        scale(2f)
    }

    fun zoomOut() {
        scale(0.5f)
    }

    /**
     * Cut currently selected audio range.
     *
     * Cut fragment will be skipped when playing, but not deleted.
     */
    fun cutSelected() {
        edit(waveform.model.editionModel::cut)
    }

    /**
     * Mute currently selected audio range.
     *
     * Muted fragment will be silenced when playing, but not changed.
     */
    fun muteSelected() {
        edit(waveform.model.editionModel::mute)
    }

    fun undo() {
        edit(waveform.model.editionModel::undo)
    }

    private inline fun edit(consumer: (ClosedLongRange) -> Unit) {
        waveform.selectionModel.selectedRanges.forEach {
            val truncated = it intersect ClosedIntRange(0, waveform.model.maxChunks - 1)
            consumer(waveform.model.chunkRangeToFrameRange(truncated))
        }
        waveform.stateChanged(ChangeEvent(waveform.model.editionModel))
    }

    /**
     * @throws java.io.IOException
     */
    fun play() {
        if (_playState == PLAY) return
        if (_playState == STOP) {
            _playState = PLAY
            val player = Player.create(waveform.file).also { this.player = it }
            player.setProcessUpdater(this::positionUpdater)
            player.play()
            ApplicationManager.getApplication().executeOnPooledThread {
                try {
                    player.applyEditions(waveform.model.editionModel)
                } catch (ex: IOException) {
                    showNotification("I/O error occurred while playing audio. ${ex.message}")
                } catch (ex: SecurityException) {
                    showNotification("Cannot access audio file due to security restrictions.")
                } finally {
                    waveform.model.playFramePosition = -1
                    stop()
                }
            }
        } else {
            player?.play()
        }
    }

    private fun positionUpdater(pos: Long) {
        waveform.model.playFramePosition = pos
        waveform.stateChanged(ChangeEvent(player))
    }

    fun pause() {
        if (_playState == STOP) return
        _playState = PAUSE
        player?.pause()
    }

    fun stop() {
        if (_playState == STOP) return
        _playState = STOP
        player?.stop()
    }

    override fun dispose() {
        stop()
    }

    private fun scale(factor: Float) {
        if (blockScaling) return
        blockScaling = true
        waveform.model.scale(factor) {
            ignoreBarChanges = true
            waveform.preferredSize = Dimension(waveform.model.maxChunks, waveform.height)
            val scrollPane = mScrollPane
            if (scrollPane != null) {
                val visible = scrollPane.viewport.visibleRect
                visible.x = waveform.model.firstVisibleChunk
                scrollPane.viewport.scrollRectToVisible(visible)
                scrollPane.horizontalScrollBar.value = visible.x
            }
            ignoreBarChanges = false
            waveform.selectionModel.resetSelection()
            waveform.model.fireStateChanged()
            revalidateRepaint()
            blockScaling = false
        }
    }

    private fun revalidateRepaint() {
        val scrollPane = mScrollPane ?: return
        scrollPane.viewport.view.revalidate()
        scrollPane.viewport.revalidate()
        scrollPane.revalidate()
        scrollPane.repaint()
        scrollPane.viewport.repaint()
        scrollPane.viewport.view.repaint()
    }
}
