package com.github.recognized.screencast.editor.view.audioview.waveform.impl

import com.github.recognized.kotlin.ranges.extensions.intersectWith
import com.github.recognized.kotlin.ranges.extensions.length
import com.github.recognized.kotlin.ranges.extensions.mapInt
import com.github.recognized.screencast.editor.view.audioview.AudioSampler
import com.github.recognized.screencast.editor.view.audioview.waveform.AveragedSampleData
import com.github.recognized.screencast.editor.view.audioview.waveform.ChangeNotifier
import com.github.recognized.screencast.editor.view.audioview.waveform.ShiftableAudioModel
import com.github.recognized.screencast.recorder.sound.SoundProvider
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CancellationException
import javax.sound.sampled.UnsupportedAudioFileException
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * @constructor
 * @throws UnsupportedAudioFileException If audio file cannot be recognized by audio system
 * or if it cannot be converted to decode format.
 * @throws java.io.IOException If I/O error occurs.
 */
class SimpleAudioModel(val getAudioStream: () -> InputStream) : ShiftableAudioModel,
  ChangeNotifier by DefaultChangeNotifier() {
  override var trackDurationMilliseconds = 0.0
    private set
  override var millisecondsPerFrame = 0.0
    private set
  override var totalFrames = 0L
    private set
  override var framesPerMillisecond = 0.0
    private set
  override var offsetFrames: Long
    get() = myOffsetFrames
    set(value) {
      myOffsetFrames = value
      fireStateChanged()
    }

  private var myOffsetFrames = 0L

  constructor(path: Path) : this({ Files.newInputStream(path) })

  init {
    SoundProvider.withWaveformPcmStream(getAudioStream()) { stream ->
      val audio = AudioSampler(stream)
      var sampleCount = 0L
      audio.forEachSample { sampleCount++ }
      totalFrames = sampleCount / stream.format.channels
      millisecondsPerFrame = 1000.0 / stream.format.frameRate
      framesPerMillisecond = stream.format.frameRate / 1000.0
      trackDurationMilliseconds = totalFrames * 1000L / stream.format.frameRate.toDouble()
    }
  }

  // Skipped chunks do not depend on offsetFrames
  override fun getAveragedSampleData(
    framesPerChunk: Int,
    chunkRange: IntRange,
    isActive: () -> Boolean
  ): List<AveragedSampleData> {
    return SoundProvider.withWaveformPcmStream(getAudioStream()) { audio ->
      val chunks = chunkRange.length
      val skippedFrames = countSkippedFrames(framesPerChunk, chunkRange)
      val readFrames = countReadFrames(framesPerChunk, chunkRange)
      val ret = List(audio.format.channels) {
        AveragedSampleData(
          (readFrames / framesPerChunk).toInt(),
          (skippedFrames / framesPerChunk).toInt(),
          audio.format.sampleSizeInBits
        )
      }
      if (chunks == 0) return@withWaveformPcmStream ret
      AudioSampler(
        audio,
        skippedFrames,
        readFrames
      ).use {
        countStat(it, framesPerChunk, ret, audio.format.channels, isActive)
      }
      ret
    }
  }

  private fun countStat(
    audio: AudioSampler,
    framesPerChunk: Int,
    data: List<AveragedSampleData>,
    channels: Int,
    isActive: () -> Boolean
  ) {
    val peaks = List(channels) { LongArray(framesPerChunk) }
    var frameCounter = 0
    var channelCounter = 0
    var chunkCounter = 0
    audio.forEachSample {
      if (!isActive()) {
        throw CancellationException()
      }
      peaks[channelCounter++][frameCounter] = it
      if (channelCounter == channels) {
        channelCounter = 0
        frameCounter++
        if (frameCounter == framesPerChunk) {
          data.forEachIndexed { index, x -> x.setChunk(frameCounter, chunkCounter, peaks[index]) }
          chunkCounter++
          frameCounter = 0
        }
      }
    }
    if (frameCounter != 0) {
      data.forEachIndexed { index, x -> x.setChunk(frameCounter, chunkCounter, peaks[index]) }
    }
  }

  private fun countReadFrames(framesPerChunk: Int, chunkRange: IntRange): Long {
    val start = Math.floorDiv(offsetFrames, framesPerChunk.toLong())
    val end = Math.floorDiv(offsetFrames + totalFrames - 1, framesPerChunk.toLong())
    val virtualChunkRange = start..end
    return (chunkRange intersectWith virtualChunkRange.mapInt { it.toInt() }).length * framesPerChunk.toLong()
  }

  private fun countSkippedFrames(framesPerChunk: Int, chunkRange: IntRange): Long {
    val start = Math.floorDiv(offsetFrames, framesPerChunk.toLong())
    val notInRead = start.toInt() until chunkRange.start
    val end = Math.floorDiv(offsetFrames + totalFrames - 1, framesPerChunk.toLong())
    val virtualChunkRange = start..end
    return (notInRead intersectWith virtualChunkRange.mapInt { it.toInt() }).length * framesPerChunk.toLong()
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
}
