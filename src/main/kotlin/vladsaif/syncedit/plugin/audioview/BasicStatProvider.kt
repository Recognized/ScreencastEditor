package vladsaif.syncedit.plugin.audioview

import vladsaif.syncedit.plugin.ClosedIntRange
import vladsaif.syncedit.plugin.floorToInt
import java.nio.file.Path
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class BasicStatProvider(file: Path) : StatProvider {
    private val file = file.toAbsolutePath().toFile()
    override var trackDurationMilliseconds = 0.0
    override var millisecondsPerFrame = 0.0
    override var totalFrames = 0L

    init {
        AudioSystem.getAudioInputStream(this.file).use {
            AudioSystem.getAudioInputStream(it.format.toDecodeFormat(), it).use {
                val audio = AudioFrameStream(it)
                var sampleCount = 0L
                audio.forEach { sampleCount++ }
                totalFrames = sampleCount / it.format.channels
                millisecondsPerFrame = it.format.millisecondsPerFrame()
                trackDurationMilliseconds = totalFrames * 1000L / it.format.frameRate.toDouble()
            }
        }
    }

    override fun getAveragedSampleData(maxChunks: Int, chunkRange: ClosedIntRange): List<AveragedSampleData> {
        val framesPerChunk = (totalFrames / maxChunks).toInt()
        AudioSystem.getAudioInputStream(file).use { input ->
            val decodeFormat = input.format.toDecodeFormat()
            val chunks = chunkRange.length
            val ret = List(decodeFormat.channels) {
                AveragedSampleData(chunks, chunkRange.start, decodeFormat.sampleSizeInBits)
            }
            if (chunks == 0) return ret
            val audio = AudioFrameStream(AudioSystem.getAudioInputStream(decodeFormat, input),
                    countSkippedFrames(maxChunks, chunkRange, framesPerChunk),
                    countReadFrames(maxChunks, chunkRange, framesPerChunk))
            countStat(audio, framesPerChunk, ret, maxChunks, chunkRange, decodeFormat.channels)
            return ret
        }
    }

    override fun getChunkOfFrame(maxChunks: Int, frame: Long): Int {
        var chunks = 0L
        val framesPerChunk = (totalFrames / maxChunks).toInt()
        if (frame > (totalFrames % framesPerChunk)) {
            chunks += (frame - (totalFrames % framesPerChunk)) / framesPerChunk
        }
        chunks += min(frame, (totalFrames % framesPerChunk)) / (framesPerChunk + 1)
        return chunks.floorToInt()
    }

    private fun countStat(audio: AudioFrameStream,
                          framesPerChunk: Int,
                          data: List<AveragedSampleData>,
                          maxChunks: Int,
                          chunkRange: ClosedIntRange,
                          channels: Int) {
        val peaks = List(channels) { LongArray(framesPerChunk + 1) }
        audio.use lit@{
            var restCounter = getBigChunkRange(maxChunks).intersect(chunkRange).length
            var frameCounter = 0
            var channelCounter = 0
            var chunkCounter = 0
            it.forEach {
                peaks[channelCounter++][frameCounter] = it
                if (channelCounter == channels) {
                    channelCounter = 0
                    frameCounter++
                    if (frameCounter == framesPerChunk && restCounter <= 0 ||
                            frameCounter == framesPerChunk + 1 && restCounter > 0) {
                        restCounter--
                        data.forEachIndexed { index, x -> x.setChunk(frameCounter, chunkCounter, peaks[index]) }
                        chunkCounter++
                        frameCounter = 0
                    }
                }
            }
            if (frameCounter != 0) {
                println(frameCounter)
                data.forEachIndexed { index, x -> x.setChunk(frameCounter, chunkCounter, peaks[index]) }
            }
        }
    }

    private fun getBigChunkRange(maxChunks: Int) = ClosedIntRange(0, (totalFrames % maxChunks).toInt() - 1)

    private fun countReadFrames(maxChunks: Int, chunkRange: ClosedIntRange, framesPerChunk: Int): Long {
        var sum = 0L
        val bigChunks = getBigChunkRange(maxChunks)
        sum += chunkRange.intersect(bigChunks).length * (framesPerChunk + 1)
        sum += chunkRange.intersect(ClosedIntRange(bigChunks.end + 1, Int.MAX_VALUE)).length * framesPerChunk
        return sum
    }

    private fun countSkippedFrames(maxChunks: Int, chunkRange: ClosedIntRange, framesPerChunk: Int): Long {
        val bigChunkRange = getBigChunkRange(maxChunks)
        val skipRange = ClosedIntRange(0, chunkRange.start - 1)
        return bigChunkRange.intersect(skipRange).length * (framesPerChunk.toLong() + 1) +
                ClosedIntRange(bigChunkRange.end + 1, chunkRange.start - 1).intersect(skipRange).length * framesPerChunk.toLong()
    }

    private fun AveragedSampleData.setChunk(counter: Int, chunk: Int, peaks: LongArray) {
        var sum = 0L
        for (j in 0 until counter) {
            sum += peaks[j]
        }
        val average = sum / counter
        var max = Long.MIN_VALUE
        for (j in 0 until counter) {
            max = max(max, peaks[j])
        }
        var min = Long.MAX_VALUE
        for (j in 0 until counter) {
            min = min(min, peaks[j])
        }
        val rmsSquared = peaks.fold(0L) { acc, x ->
            acc + (x - average) * (x - average)
        }.toDouble() / counter
        averagePeaks[chunk] = average
        rootMeanSquare[chunk] = sqrt(rmsSquared).toLong()
        highestPeaks[chunk] = max
        lowestPeaks[chunk] = min
    }

    private fun AudioFormat.millisecondsPerFrame() = 1000.0 / frameRate

    private fun AudioFormat.toDecodeFormat() =
            AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                    sampleRate,
                    16,
                    channels,
                    channels * 2,
                    sampleRate,
                    false)

}
