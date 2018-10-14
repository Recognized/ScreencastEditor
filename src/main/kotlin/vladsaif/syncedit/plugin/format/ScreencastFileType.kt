package vladsaif.syncedit.plugin.format

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import icons.ScreencastEditorIcons
import javax.swing.Icon

object ScreencastFileType : FileType {
  override fun getDefaultExtension() = "scs"

  override fun getIcon(): Icon = ScreencastEditorIcons.SCREENCAST

  override fun getCharset(file: VirtualFile, content: ByteArray): String? = null

  override fun getName() = "Screencast"

  override fun getDescription() = "Screencast Data File"

  override fun isBinary() = true

  override fun isReadOnly() = true

  val dotExtension = ".$defaultExtension"
}