package vladsaif.syncedit.plugin.diffview

interface Locator {

  fun locate(item: Int): Pair<Int, Int>
}