package vladsaif.syncedit.plugin.lang.script.psi

import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import javax.swing.Icon

object UIScriptFileType : LanguageFileType(KotlinLanguage.INSTANCE) {
  override fun getIcon(): Icon? = KotlinFileType.INSTANCE.icon

  override fun getName(): String = "UI Script"

  override fun getDefaultExtension(): String = "guitest"

  override fun getDescription(): String = "UI Script"
}