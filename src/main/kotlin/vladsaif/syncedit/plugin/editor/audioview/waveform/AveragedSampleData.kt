package vladsaif.syncedit.plugin.editor.audioview.waveform

import java.util.*

class AveragedSampleData(val size: Int, val skippedChunks: Int, val sampleSizeInBits: Int) {
  val highestPeaks = LongArray(size)
  val lowestPeaks = LongArray(size)
  val averagePeaks = LongArray(size)
  val rootMeanSquare = LongArray(size)
  val maxPeak get() = 1L shl (sampleSizeInBits - 1)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AveragedSampleData

    if (size != other.size) return false
    if (skippedChunks != other.skippedChunks) return false
    if (sampleSizeInBits != other.sampleSizeInBits) return false
    if (!highestPeaks.contentEquals(other.highestPeaks)) return false
    if (!lowestPeaks.contentEquals(other.lowestPeaks)) return false
    if (!averagePeaks.contentEquals(other.averagePeaks)) return false
    if (!rootMeanSquare.contentEquals(other.rootMeanSquare)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = size
    result = 31 * result + skippedChunks
    result = 31 * result + sampleSizeInBits
    result = 31 * result + highestPeaks.contentHashCode()
    result = 31 * result + lowestPeaks.contentHashCode()
    result = 31 * result + averagePeaks.contentHashCode()
    result = 31 * result + rootMeanSquare.contentHashCode()
    return result
  }

  override fun toString(): String {
    return "AveragedSampleData(size=$size, skippedChunks=$skippedChunks, sampleSizeInBits=$sampleSizeInBits, highestPeaks=${Arrays.toString(
      highestPeaks
    )}, lowestPeaks=${Arrays.toString(lowestPeaks)}, averagePeaks=${Arrays.toString(averagePeaks)}, rootMeanSquare=${Arrays.toString(
      rootMeanSquare
    )})"
  }


  companion object {
    val ZERO = AveragedSampleData(0, 0, 16)
  }
}