package vladsaif.syncedit.plugin.lang.transcript.psi

import com.intellij.ide.highlighter.XmlFileType
import com.intellij.lang.Language
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.fileTypes.LanguageFileType
import java.util.*
import javax.swing.Icon

object TranscriptViewLanguage : Language("TranscriptViewLanguage")

// Do not create file of this file type manually
object TranscriptFileType : LanguageFileType(TranscriptViewLanguage) {
    private val extension = UUID.randomUUID().toString()

    override fun getName() = "Transcript file"

    override fun getDescription() = "Transcript PSI Skeleton"

    override fun getDefaultExtension() = extension

    override fun getIcon(): Icon? = null
}

class TranscriptFileTypeFactory : FileTypeFactory() {
    override fun createFileTypes(consumer: FileTypeConsumer) {
        consumer.consume(TranscriptFileType)
    }
}

object InternalFileType : LanguageFileType(XMLLanguage.INSTANCE) {
    override fun getIcon() = XmlFileType.INSTANCE.icon

    override fun getName() = "Transcript"

    override fun getDefaultExtension() = "transcript"

    override fun getDescription() = "Transcript file type"
}

class InternalTranscriptFileTypeFactory : FileTypeFactory() {
    override fun createFileTypes(consumer: FileTypeConsumer) {
        consumer.consume(InternalFileType)
    }
}