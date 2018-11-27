package vladsaif.syncedit.plugin.util

import com.intellij.util.ui.JBUI
import kotlin.math.roundToInt

fun Int.modFloor(modulus: Int): Int {
  return this - this % modulus
}

fun Int.divScale(): Int {
  return (this / JBUI.pixScale()).roundToInt()
}

fun Int.mulScale(): Int {
  return (JBUI.pixScale(this.toFloat())).roundToInt()
}

fun Int.divScaleF(): Float {
  return this / JBUI.pixScale()
}

fun Int.mulScaleF(): Float {
  return JBUI.pixScale(this.toFloat())
}

infix fun Long.ceil(other: Long) = (this + other - 1) / other
