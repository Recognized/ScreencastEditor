package vladsaif.syncedit.plugin.diffview

import vladsaif.syncedit.plugin.IRange

class BindProvider {
  fun getBindings(): Map<IRange, IRange> {
    return mapOf(IRange(1, 2) to IRange(1, 5))
  }
}