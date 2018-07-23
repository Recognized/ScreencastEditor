package vladsaif.syncedit.plugin.lang.transcript.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner

class TranscriptFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, TranscriptViewLanguage) {

    override fun getFileType() = TranscriptViewFileType

    override fun toString() = "Transcript file"
}

interface TranscriptWord : PsiNameIdentifierOwner {
    val number: Int
}

internal class TranscriptWordImpl(node: ASTNode, override val number: Int) : ASTWrapperPsiElement(node), TranscriptWord {
    override fun getNameIdentifier() = this

    override fun setName(name: String): PsiElement {
        println("name = [${name}]")
        return this
    }
}