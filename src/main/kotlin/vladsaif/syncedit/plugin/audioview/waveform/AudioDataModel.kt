package vladsaif.syncedit.plugin.audioview.waveform

import vladsaif.syncedit.plugin.util.IRange
import vladsaif.syncedit.plugin.util.LRange
import javax.sound.sampled.AudioFormat

interface AudioDataModel {
  /**
   * @return Total duration of the track in milliseconds.
   */
  val trackDurationMilliseconds: Double
  /**
   * @return Length of frame in milliseconds.
   */
  val millisecondsPerFrame: Double
  /**
   * @return Total number of frames in this audio file.
   */
  val totalFrames: Long
  /**
   * @return Number of frames in one millisecond
   */
  val framesPerMillisecond: Double

  /**
   * @throws java.io.IOException If I/O error occurs.
   * @return Averaged audio data for each chunk in [chunkRange].
   * Chunk is a sequence of XX or XX + 1 samples, where XX = [totalFrames] / [maxChunks].
   */
  fun getAveragedSampleData(maxChunks: Int, chunkRange: IRange, isActive: () -> Boolean): List<AveragedSampleData>

  /**
   * @return Number of chunk (if all frame were split into [maxChunks] number of chunks)
   * starting from zero, which contains [frame].
   */
  fun getChunk(maxChunks: Int, frame: Long): Int

  /**
   * @return First frame of the [chunk] if all frames were split in [maxChunks] number of chunks.
   */
  fun getStartFrame(maxChunks: Int, chunk: Int): Long

  fun msRangeToFrameRange(range: IRange): LRange

  fun frameRangeToMsRange(range: LRange): IRange
}

fun AudioFormat.toDecodeFormat() =
    AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
        sampleRate,
        16,
        channels,
        channels * 2,
        sampleRate,
        false)