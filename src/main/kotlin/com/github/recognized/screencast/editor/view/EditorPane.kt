package com.github.recognized.screencast.editor.view

import com.github.recognized.screencast.editor.model.Screencast
import com.github.recognized.screencast.editor.view.audioview.StubPanel
import com.github.recognized.screencast.editor.view.audioview.WaveformGraphics
import com.github.recognized.screencast.editor.view.audioview.waveform.DraggableXAxis
import com.github.recognized.screencast.editor.view.audioview.waveform.Waveform
import com.github.recognized.screencast.editor.view.scriptview.ScriptView
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.Divider
import com.intellij.openapi.ui.OnePixelDivider
import com.intellij.openapi.util.Disposer
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBScrollPane
import java.awt.Graphics
import java.awt.event.MouseEvent
import javax.swing.ScrollPaneConstants
import javax.swing.event.ChangeListener
import javax.swing.event.MouseInputAdapter

class EditorPane(
  val screencast: Screencast
) : JBScrollPane(), Disposable {

  private val myScriptView = ScriptView(screencast)
  private val myWaveforms = mutableListOf<Waveform>()
  private val mySplitter: EditorSplitter
  private var myActiveWaveform: Waveform? = null
    set(value) {
      field?.view?.isSelected = false
      field?.view?.repaint()
      value?.view?.isSelected = true
      value?.view?.repaint()
      field = value
    }
  private val myAudioListener: () -> Unit = {
    recreateWaveforms()
  }
  val zoomController = ZoomController(screencast.coordinator)
  var isXAxisDraggingEnabled = false
    private set

  init {
    mySplitter = EditorSplitter(
      StubPanel(),
      myScriptView,
      screencast.coordinator
    )
    setViewportView(mySplitter)
    recreateWaveforms()
    Disposer.register(this, myScriptView)
    verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
    horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
    myScriptView.installListeners()
    zoomController.install(this)
    zoomController.addChangeListener(ChangeListener {
      reset()
    })
    screencast.addAudioListener(myAudioListener)
  }

  fun toggleDragXAxis() {
    val draggable: List<DraggableXAxis> = myWaveforms.map { it.view } + myScriptView
    if (isXAxisDraggingEnabled) {
      draggable.forEach(DraggableXAxis::deactivateXAxisDrag)
    } else {
      draggable.forEach(DraggableXAxis::activateXAxisDrag)
    }
    isXAxisDraggingEnabled = isXAxisDraggingEnabled xor true
  }

  private fun reset() {
    myScriptView.resetCache()
    for (waveformView in myWaveforms) {
      waveformView.view.selectionModel.resetSelection()
      waveformView.model.resetCache()
    }
    mySplitter.updateInterval()
  }

  private fun recreateWaveforms() {
    val currentAudios = listOfNotNull(screencast.pluginAudio, screencast.importedAudio)
    val retainedWaveforms = myWaveforms.filter { it.model.audio in currentAudios }
    val newWaveforms = currentAudios.filter { it !in retainedWaveforms.map { x -> x.model.audio } }
      .map { Waveform.create(screencast, it) }
    newWaveforms.forEach {
      it.view.installListeners()
      it.view.addMouseListener(ActiveWaveformListener(it))
    }
    val currentWaveforms = retainedWaveforms + newWaveforms
    val newViewport = when (currentWaveforms.size) {
      2 -> {
        val splitter = if (mySplitter.firstComponent is WaveformSplitter) mySplitter.firstComponent as WaveformSplitter
        else WaveformSplitter()
        splitter.firstComponent = currentWaveforms[0].view
        splitter.secondComponent = currentWaveforms[1].view
        splitter
      }
      1 -> currentWaveforms[0].view
      else -> StubPanel()
    }
    mySplitter.firstComponent = newViewport
    myWaveforms.filter { it !in currentWaveforms }.forEach { Disposer.dispose(it) }
    myWaveforms.clear()
    myWaveforms.addAll(currentWaveforms)
    if (myActiveWaveform !in myWaveforms) {
      myActiveWaveform = null
    }
    revalidate()
    repaint()
  }

  private class WaveformSplitter : OnePixelSplitter(true, 0.5f, 0.2f, 0.8f) {

    override fun createDivider(): Divider {
      return object : OnePixelDivider(true, this@WaveformSplitter) {

        override fun paintComponent(g: Graphics) {
          g.color = WaveformGraphics.HORIZONTAL_LINE
          g.fillRect(0, 0, this.width, this.height)
        }
      }
    }
  }

  val hasSelection get() = myActiveWaveform?.controller?.hasSelection == true

  val playState get() = myActiveWaveform?.controller?.playState

  fun getActiveAudio(): Screencast.Audio? {
    return myActiveWaveform?.model?.audio
  }

  fun play() {
    myActiveWaveform?.controller?.play()
  }

  fun pause() {
    myActiveWaveform?.controller?.pause()
  }

  fun stop() {
    myActiveWaveform?.controller?.stopImmediately()
  }

  fun cut() {
    myActiveWaveform?.controller?.cutSelected()
    myActiveWaveform?.view?.selectionModel?.resetSelection()
  }

  fun mute() {
    myActiveWaveform?.controller?.muteSelected()
    myActiveWaveform?.view?.selectionModel?.resetSelection()
  }

  fun unmute() {
    myActiveWaveform?.controller?.unmuteSelected()
    myActiveWaveform?.view?.selectionModel?.resetSelection()
  }

  private inner class ActiveWaveformListener(val waveform: Waveform) : MouseInputAdapter() {
    override fun mouseClicked(e: MouseEvent?) {
      myActiveWaveform = waveform
    }

    override fun mousePressed(e: MouseEvent?) {
      myActiveWaveform = waveform
    }
  }

  override fun dispose() {
    for (waveform in myWaveforms) {
      waveform.controller.stop()
    }
    screencast.removeAudioListener(myAudioListener)
  }
}