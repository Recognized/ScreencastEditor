package vladsaif.syncedit.plugin.lang.transcript.psi

import com.intellij.lang.ASTNode
import com.intellij.lang.LightPsiParser
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.openapi.util.Key
import com.intellij.psi.tree.IElementType

class TranscriptElementType(debugName: String) : IElementType(debugName, TranscriptViewLanguage)

class TranscriptTokenType(debugName: String) : IElementType(debugName, TranscriptViewLanguage) {
    override fun toString(): String {
        return "TranscriptTokenType." + super.toString()
    }
}

class ManualParser : PsiParser, LightPsiParser {
    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        parseLight(root, builder)
        return builder.treeBuilt
    }

    override fun parseLight(root: IElementType?, builder: PsiBuilder?) {
        root ?: return
        builder ?: return
        val start = builder.mark()
        while(builder.tokenType != null) {
            val mark = builder.mark()
            builder.advanceLexer()
            mark.done(WORD)
        }
        start.done(root)
    }

    companion object {
        val WORD: IElementType = TranscriptElementType("WORD")
        val WORD_TOKEN: IElementType = TranscriptTokenType("WORD_TOKEN")
        val NUMBER_KEY: Key<Int> = Key("number_among_children_of_parent")
    }
}
