package vladsaif.syncedit.plugin.lang.transcript.refactoring

import vladsaif.syncedit.plugin.MultimediaModel
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptWord

class MuteAction : IncludeExcludeActionBase() {

    override fun doAction(model: MultimediaModel, words: List<TranscriptWord>) {
        model.muteWords(words.map { it.number }.toIntArray())
    }
}
