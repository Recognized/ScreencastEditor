package vladsaif.syncedit.plugin.audioview

import vladsaif.syncedit.plugin.ClosedIntRange

interface StatProvider {
    val trackDurationMilliseconds: Double
    val millisecondsPerFrame: Double
    val totalFrames: Long

    fun getAveragedSampleData(maxChunks: Int, chunkRange: ClosedIntRange) : List<AveragedSampleData>

    fun getChunkOfFrame(maxChunks: Int, frame: Long): Int
}

