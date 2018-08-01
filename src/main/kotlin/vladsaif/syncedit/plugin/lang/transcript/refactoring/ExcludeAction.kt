package vladsaif.syncedit.plugin.lang.transcript.refactoring

import vladsaif.syncedit.plugin.TranscriptModel
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptWord

/**
 * Excludes words from transcript so they are cut out from corresponding audio.
 * @see IncludeAction to revert changes
 */
class ExcludeAction : IncludeExcludeActionBase() {

    override fun doAction(model: TranscriptModel, words: List<TranscriptWord>) {
        model.excludeWords(words.map { it.number }.toIntArray())
    }
}
