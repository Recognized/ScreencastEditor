package vladsaif.syncedit.plugin

data class Word(val text: String, val startMillisecond: Double, val endMilliseconds: Double) : Comparable<Word> {
    override operator fun compareTo(other: Word) = startMillisecond.compareTo(other.startMillisecond)
}