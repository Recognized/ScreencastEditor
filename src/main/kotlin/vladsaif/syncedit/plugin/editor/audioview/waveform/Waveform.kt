package vladsaif.syncedit.plugin.editor.audioview.waveform

import vladsaif.syncedit.plugin.model.ScreencastFile

data class Waveform(val model: WaveformModel, val view: WaveformView, val controller: WaveformController) {

  companion object {
    fun create(screencastFile: ScreencastFile, audioDataModel: AudioDataModel): Waveform {
      val model = WaveformModel(screencastFile, audioDataModel)
      val view = WaveformView(model)
      val controller = WaveformController(view)
      return Waveform(model, view, controller)
    }
  }
}