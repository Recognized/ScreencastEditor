package vladsaif.syncedit.plugin.lang.script.parser

import com.intellij.lang.ParserDefinition
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.parsing.KotlinParserDefinition
import vladsaif.syncedit.plugin.lang.script.psi.UIScriptFile

class UIScriptParserDefinition : ParserDefinition by KotlinParserDefinition.instance {

  override fun createFile(viewProvider: FileViewProvider): PsiFile {
    return UIScriptFile(viewProvider)
  }
}