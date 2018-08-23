package vladsaif.syncedit.plugin

import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader
import java.io.File
import javax.sound.sampled.*

/** This object duplicates some part of [javax.sound.sampled.AudioSystem].
 *
 * It exists because of some unknown reasons in Intellij Platform and bad SPI design,
 * which does not allow to manually load implementation classes from code,
 * but only to load them from reading specially formatted files in META-INF/services directory.
 */
object SoundProvider {
  private val MPEG_PROVIDER = MpegFormatConversionProvider()
  private val MPEG_FILE_READER = MpegAudioFileReader()

  /**
   * @throws java.io.IOException
   * @throws UnsupportedAudioFileException
   */
  fun getAudioInputStream(file: File): AudioInputStream {
    return try {
      AudioSystem.getAudioInputStream(file)
    } catch (ex: UnsupportedAudioFileException) {
      MPEG_FILE_READER.getAudioInputStream(file)
    }
  }

  fun isConversionSupported(targetFormat: AudioFormat, sourceFormat: AudioFormat): Boolean {
    return AudioSystem.isConversionSupported(targetFormat, sourceFormat)
        || MPEG_PROVIDER.isConversionSupported(targetFormat, sourceFormat)
  }

  /**
   * @throws IllegalArgumentException if conversion is not supported.
   */
  fun getAudioInputStream(targetFormat: AudioFormat, stream: AudioInputStream): AudioInputStream {
    return if (MPEG_PROVIDER.isConversionSupported(targetFormat, stream.format)) {
      MPEG_PROVIDER.getAudioInputStream(targetFormat, stream)
    } else AudioSystem.getAudioInputStream(targetFormat, stream)
  }

  /**
   * @throws java.io.IOException
   * @throws UnsupportedAudioFileException
   */
  fun getAudioFileFormat(file: File): AudioFileFormat {
    return try {
      AudioSystem.getAudioFileFormat(file)
    } catch (ex: UnsupportedAudioFileException) {
      MPEG_FILE_READER.getAudioFileFormat(file)
    }
  }
}