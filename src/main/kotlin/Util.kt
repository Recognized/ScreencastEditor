package vladsaif.syncedit.plugin

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.diagnostic.logger
import javax.sound.sampled.AudioFormat
import kotlin.math.max
import kotlin.math.min

fun showNotification(
        content: String,
        title: String = "Error",
        type: NotificationType = NotificationType.ERROR
) {
    Notifications.Bus.notify(Notification("Screencast Editor", title, content, type))
}

fun Long.floorToInt(): Int {
    return if (this > 0) min(this, Int.MAX_VALUE.toLong()).toInt()
    else max(this, Int.MIN_VALUE.toLong()).toInt()
}

inline fun <reified T : Any> T.getLog() = logger(T::class.java.name)

private fun LongArray.averageN(n: Int): LongArray {
    val ret = LongArray(size)
    var sum = 0L
    for (i in -n until size + n) {
        if (i + n in 0..(size - 1)) {
            sum += this[i + n]
        }
        if (i - n - 1 in 0..(size - 1)) {
            sum -= this[i - n - 1]
        }
        if (i in 0..(size - 1)) {
            ret[i] = sum / (n * 2 + 1)
        }
    }
    return ret
}

fun Int.modFloor(modulus: Int): Int {
    return this - this % modulus
}

fun AudioFormat.toDecodeFormat() =
        AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                sampleRate,
                16,
                channels,
                channels * 2,
                sampleRate,
                false)

