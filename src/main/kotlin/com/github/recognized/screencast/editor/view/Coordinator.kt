package com.github.recognized.screencast.editor.view

import com.github.recognized.kotlin.ranges.extensions.floorToInt
import com.github.recognized.kotlin.ranges.extensions.mapInt
import com.github.recognized.kotlin.ranges.extensions.mapLong
import com.github.recognized.screencast.editor.view.audioview.waveform.ChangeNotifier
import com.github.recognized.screencast.editor.view.audioview.waveform.impl.DefaultChangeNotifier
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class Coordinator : ChangeNotifier by DefaultChangeNotifier() {
  private var nsPerFrame: Double = 1_000_000_000.0 / 44100
  private val nsPerPixel get() = nsPerFrame * framesPerPixel
  var visibleRange: IntRange = 0 until 1920
    set(value) {
      field = value
      fireStateChanged()
    }
  var framesPerPixel: Long = 441L
    set(value) {
      field = value.coerceIn(ALLOWED_FRAMES_PER_PIXEL)
      fireStateChanged()
    }
  var framesPerSecond: Long = 44100L
    set(value) {
      field = value
      nsPerFrame = 1_000_000_000.0 / value
      fireStateChanged()
    }

  fun toPixel(time: Long, unit: TimeUnit): Int {
    return Math.floor(TimeUnit.NANOSECONDS.convert(time, unit) / nsPerPixel).roundToInt()
  }

  fun toPixel(time: Int, unit: TimeUnit): Int {
    return Math.floor(TimeUnit.NANOSECONDS.convert(time.toLong(), unit) / nsPerPixel).roundToInt()
  }

  fun toPixel(frame: Long): Int {
    return Math.floorDiv(frame, framesPerPixel).floorToInt()
  }

  fun toFrame(pixel: Int): Long {
    return pixel * framesPerPixel
  }

  fun toPixelRange(frameRange: LongRange): IntRange {
    return frameRange.mapInt(this::toPixel)
  }

  fun toPixelRange(time: IntRange, unit: TimeUnit): IntRange {
    return time.mapInt { toPixel(it, unit) }
  }

  fun toFrame(time: Long, timeUnit: TimeUnit): Long {
    return (Math.floor(TimeUnit.NANOSECONDS.convert(time, timeUnit) / nsPerFrame)).roundToLong()
  }

  fun toFrameRange(time: LongRange, timeUnit: TimeUnit): LongRange {
    return time.mapLong { toFrame(it, timeUnit) }
  }

  fun toFrameRange(time: IntRange, timeUnit: TimeUnit): LongRange {
    return time.mapLong { toFrame(it.toLong(), timeUnit) }
  }

  fun toNanoseconds(pixel: Int): Long {
    return (pixel * nsPerPixel).toLong()
  }

  fun toMilliseconds(frame: Long): Int {
    return (Math.floor(frame * nsPerFrame / 1_000_000)).roundToInt()
  }

  fun toMillisecondsRange(frameRange: LongRange): IntRange {
    return frameRange.mapInt(this::toMilliseconds)
  }

  fun toFrameRange(pixelRange: IntRange): LongRange {
    return pixelRange.mapLong(this::toFrame)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Coordinator

    if (nsPerFrame != other.nsPerFrame) return false
    if (visibleRange != other.visibleRange) return false
    if (framesPerPixel != other.framesPerPixel) return false
    if (framesPerSecond != other.framesPerSecond) return false

    return true
  }

  override fun hashCode(): Int {
    var result = nsPerFrame.hashCode()
    result = 31 * result + visibleRange.hashCode()
    result = 31 * result + framesPerPixel.hashCode()
    result = 31 * result + framesPerSecond.hashCode()
    return result
  }

  override fun toString(): String {
    return "Coordinator(nsPerFrame=$nsPerFrame, visibleRange=$visibleRange, framesPerPixel=$framesPerPixel, framesPerSecond=$framesPerSecond)"
  }

  companion object {
    private val ALLOWED_FRAMES_PER_PIXEL: LongRange = 20L..100000L
  }
}