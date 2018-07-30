package vladsaif.syncedit.plugin.recognition

import vladsaif.syncedit.plugin.TranscriptData
import vladsaif.syncedit.plugin.recognition.recognizers.GSpeechKit
import java.io.IOException
import java.io.InputStream

interface SpeechRecognizer {

    /**
     * @return Recognized [TranscriptData] in audio from [inputStream].
     */
    @Throws(IOException::class)
    suspend fun recognize(inputStream: InputStream): TranscriptData

    companion object {

        @Throws(IOException::class)
        suspend fun getDefault(credentials: InputStream): SpeechRecognizer {
            return GSpeechKit.create(credentials)
        }
    }
}
