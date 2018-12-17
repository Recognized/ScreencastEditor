package com.github.recognized.screencast.editor.view.audioview.waveform

import com.github.recognized.screencast.editor.model.Screencast
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer

data class Waveform(val model: WaveformModel, val view: WaveformView, val controller: WaveformController) : Disposable {

  override fun dispose() {
  }

  companion object {
    fun create(screencast: Screencast, audio: Screencast.Audio): Waveform {
      val model = WaveformModel(screencast, audio)
      val view = WaveformView(model)
      val controller = WaveformController(view)
      return Waveform(model, view, controller).also {
        Disposer.register(it, model)
      }
    }
  }
}