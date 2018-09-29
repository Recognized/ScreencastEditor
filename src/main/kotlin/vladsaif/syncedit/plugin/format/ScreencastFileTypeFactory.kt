package vladsaif.syncedit.plugin.format

import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory

class ScreencastFileTypeFactory : FileTypeFactory() {

  override fun createFileTypes(consumer: FileTypeConsumer) {
    consumer.consume(ScreencastFileType)
  }
}