package vladsaif.syncedit.plugin.audioview

import vladsaif.syncedit.plugin.ClosedLongRange

typealias TimeMillis = Double
typealias AveragedSampleData = SampleProvider.AveragedSampleData

interface SampleProvider {
    val trackDuration: TimeMillis
    val millisecondsPerFrame: TimeMillis
    val totalFrames: Int

    fun getAveragedSampleData(maxChunks: Long, chunkRange: ClosedLongRange) : List<AveragedSampleData>

    fun getChunkOfFrame(maxChunks: Long, frame: Long): Int

    class AveragedSampleData(val size: Int, val skippedChunks: Long, sampleSizeInBits: Int) {
        val maxPeak = 1L shl (sampleSizeInBits - 1)
        val highestPeaks = LongArray(size)
        val lowestPeaks = LongArray(size)
        val averagePeaks = LongArray(size)
        val rootMeanSquare = LongArray(size)

        override fun toString(): String {
            return "Peaks($size): ${highestPeaks.take(30)}\n Rms: ${rootMeanSquare.take(30)}"
        }
    }
}

