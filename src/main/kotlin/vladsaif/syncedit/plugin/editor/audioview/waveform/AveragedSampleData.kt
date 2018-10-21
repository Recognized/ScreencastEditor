package vladsaif.syncedit.plugin.editor.audioview.waveform

data class AveragedSampleData(val size: Int, val skippedChunks: Int, val sampleSizeInBits: Int) {
  val highestPeaks = LongArray(size)
  val lowestPeaks = LongArray(size)
  val averagePeaks = LongArray(size)
  val rootMeanSquare = LongArray(size)
  val maxPeak
    get() = 1L shl (sampleSizeInBits - 1)

  constructor() : this(0, 0, 16)
}