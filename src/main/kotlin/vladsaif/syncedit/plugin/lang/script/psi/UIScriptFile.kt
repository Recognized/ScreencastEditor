package vladsaif.syncedit.plugin.lang.script.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class UIScriptFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, UIScriptLanguage) {
  override fun getFileType(): FileType {
    return UIScriptFileType
  }
}