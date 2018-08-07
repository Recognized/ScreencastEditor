package vladsaif.syncedit.plugin.lang.script.psi

import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.kotlin.idea.KotlinFileType
import javax.swing.Icon

object UIScriptFileType : LanguageFileType(UIScriptLanguage) {
  override fun getIcon(): Icon? = KotlinFileType.INSTANCE.icon

  override fun getName(): String = "UI Script"

  override fun getDefaultExtension(): String = "guitest"

  override fun getDescription(): String = "UI Script"
}