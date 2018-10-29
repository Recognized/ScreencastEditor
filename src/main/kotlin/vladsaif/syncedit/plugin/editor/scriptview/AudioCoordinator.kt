package vladsaif.syncedit.plugin.editor.scriptview

import vladsaif.syncedit.plugin.editor.audioview.waveform.AudioDataModel
import vladsaif.syncedit.plugin.util.end
import java.util.concurrent.TimeUnit

class AudioCoordinator(private val audioDataModel: AudioDataModel) : Coordinator {
  var maxPixels = 4000

  override fun toScreenPixel(time: Long, unit: TimeUnit): Int {
    return getPixel(
      (TimeUnit.MILLISECONDS.convert(time, unit) * audioDataModel.framesPerMillisecond).toLong()
    )
  }

  override fun toNanoseconds(pixel: Int): Long {
    val startFrame = audioDataModel.getStartFrame(maxPixels, pixel)
    return ((startFrame / audioDataModel.framesPerMillisecond) * 1_000_000).toLong()
  }

  fun getPixel(frame: Long) = audioDataModel.getChunk(maxPixels, frame)

  fun pixelRangeToFrameRange(pixelRange: IntRange): LongRange {
    return LongRange(
      audioDataModel.getStartFrame(maxPixels, pixelRange.start),
      audioDataModel.getStartFrame(maxPixels, pixelRange.end + 1) - 1
    )
  }

  fun frameRangeToPixelRange(frameRange: LongRange): IntRange {
    return IntRange(
      audioDataModel.getChunk(maxPixels, frameRange.start),
      audioDataModel.getChunk(maxPixels, frameRange.end)
    )
  }
}