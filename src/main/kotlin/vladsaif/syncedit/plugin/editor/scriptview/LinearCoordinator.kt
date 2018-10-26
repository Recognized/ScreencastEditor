package vladsaif.syncedit.plugin.editor.scriptview

import java.util.concurrent.TimeUnit

class LinearCoordinator : Coordinator {
  // Number of nanoseconds per pixel
  private var myNsPerDot: Long = 1_000_000_000

  fun setTimeUnitsPerPixel(newValue: Long, unit: TimeUnit) {
    myNsPerDot = TimeUnit.NANOSECONDS.convert(newValue, unit)
  }

  fun getTimeUnitsPerPixel(unit: TimeUnit): Long {
    return unit.convert(myNsPerDot, TimeUnit.NANOSECONDS)
  }

  override fun toScreenPixel(time: Long, unit: TimeUnit): Int {
    return (TimeUnit.NANOSECONDS.convert(time, unit) / myNsPerDot).toInt()
  }

  override fun toNanoseconds(pixel: Int): Long {
    return pixel * myNsPerDot
  }
}