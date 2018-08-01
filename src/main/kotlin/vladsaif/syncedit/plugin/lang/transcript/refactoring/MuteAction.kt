package vladsaif.syncedit.plugin.lang.transcript.refactoring

import vladsaif.syncedit.plugin.TranscriptModel
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptWord

class MuteAction : IncludeExcludeActionBase() {

    override fun doAction(model: TranscriptModel, words: List<TranscriptWord>) {
        model.muteWords(words.map { it.number }.toIntArray())
    }
}
