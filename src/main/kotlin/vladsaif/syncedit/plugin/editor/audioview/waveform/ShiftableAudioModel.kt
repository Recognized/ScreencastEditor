package vladsaif.syncedit.plugin.editor.audioview.waveform

interface AudioModel : ChangeNotifier {
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

  val offsetFrames: Long

  fun getAveragedSampleData(
    framesPerChunk: Int,
    chunkRange: IntRange,
    isActive: () -> Boolean
  ): List<AveragedSampleData>
}

interface ShiftableAudioModel : AudioModel {

  /**
   * Offset of the first frame
   */
  override var offsetFrames: Long

}

