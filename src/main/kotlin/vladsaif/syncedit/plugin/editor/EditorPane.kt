package vladsaif.syncedit.plugin.editor

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import vladsaif.syncedit.plugin.editor.audioview.waveform.DraggableXAxis
import vladsaif.syncedit.plugin.editor.audioview.waveform.Waveform
import vladsaif.syncedit.plugin.editor.scriptview.ScriptView
import vladsaif.syncedit.plugin.model.Screencast
import java.awt.Dimension
import java.awt.event.MouseEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.ScrollPaneConstants
import javax.swing.event.ChangeListener
import javax.swing.event.MouseInputAdapter

class EditorPane(
  screencast: Screencast
) : JBScrollPane(), Disposable {

  private val myScriptView = ScriptView(screencast)
  private val myWaveforms = mutableListOf<Waveform>()
  private val myWaveformsContainer: Box = Box(BoxLayout.Y_AXIS)
  private val mySplitter: EditorSplitter
  private var myActiveWaveform: Waveform? = null
  val zoomController = ZoomController(screencast.coordinator)
  var isXAxisDraggingEnabled = false
    private set

  init {
    mySplitter = EditorSplitter(
      myWaveformsContainer,
      myScriptView,
      myScriptView.coordinator
    )
    setViewportView(mySplitter)
    screencast.pluginAudio?.let {
      addWaveform(Waveform.create(screencast, it))
    }
    screencast.importedAudio?.let {
      addWaveform(Waveform.create(screencast, it))
    }
    Disposer.register(this, myScriptView)
    verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
    horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
    myScriptView.installListeners()
    zoomController.install(this)
    zoomController.addChangeListener(ChangeListener {
      reset()
    })
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
      waveformView.model.drawRange.resetCache()
      waveformView.view.selectionModel.resetSelection()
      waveformView.model.resetCache()
    }
    mySplitter.updateInterval()
  }

  fun addWaveform(waveform: Waveform) {
    if (myWaveforms.contains(waveform)) return
    Disposer.register(this, waveform)
    myWaveforms.add(waveform)
    myActiveWaveform = waveform
    if (myWaveforms.size > 1) {
      myWaveformsContainer.add(Box.createRigidArea(Dimension(width, JBUI.scale(5))))
    }
    waveform.view.addMouseListener(ActiveWaveformListener(waveform))
    myWaveformsContainer.add(waveform.view)
    waveform.view.installListeners()
  }

  fun removeWaveform(waveform: Waveform) {
    myWaveforms.remove(waveform)
    val index = myWaveformsContainer.components.indexOf(waveform.view)
    myWaveformsContainer.remove(waveform.view)
    if (index != 0) {
      myWaveformsContainer.remove(index - 1)
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
  }
}