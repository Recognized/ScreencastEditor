package vladsaif.syncedit.plugin

import vladsaif.syncedit.plugin.audioview.TimeMillis

data class Word(val text: String, val timeStart: TimeMillis, val timeEnd: TimeMillis)