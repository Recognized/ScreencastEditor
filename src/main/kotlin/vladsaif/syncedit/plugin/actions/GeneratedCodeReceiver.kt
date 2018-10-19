package vladsaif.syncedit.plugin.actions

import com.intellij.testGuiFramework.recorder.GeneratedCodeReceiver

object GeneratedCodeReceiver : GeneratedCodeReceiver {
  private var myBuilder = StringBuilder();

  @Synchronized
  override fun receiveCode(code: String, indentation: Int) {
    if (code != "}" && Timer.offsetToLastStatement >= 16) {
      myBuilder.appendln(Timer.newTimeOffsetStatement())
    }
    myBuilder.append("  " * indentation + code)
    if (code == "}" && Timer.offsetToLastStatement >= 16) {
      myBuilder.appendln(Timer.newTimeOffsetStatement())
    }
  }

  @Synchronized
  fun getAndFlush(): String {
    return myBuilder.toString().also { myBuilder = StringBuilder() }
  }
}

infix operator fun String.times(multiplier: Int): String {
  return buildString {
    for (i in 1..multiplier) {
      append(this)
    }
  }
}