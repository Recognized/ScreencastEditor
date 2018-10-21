package vladsaif.syncedit.plugin.editor.scriptview

import vladsaif.syncedit.plugin.util.end
import java.util.concurrent.TimeUnit

interface Coordinator {
  fun toScreenPixel(time: Long, unit: TimeUnit): Int

  fun toScreenPixel(nsRange: LongRange, unit: TimeUnit): IntRange {
    return toScreenPixel(nsRange.start, unit)..toScreenPixel(nsRange.end, unit)
  }

  fun toNanoseconds(pixel: Int): Long

  fun toNanoseconds(pixelRange: IntRange): LongRange {
    return toNanoseconds(pixelRange.start)..toNanoseconds(pixelRange.end)
  }
}