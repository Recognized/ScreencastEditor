package vladsaif.syncedit.plugin.audioview.waveform.impl

import com.intellij.openapi.application.ApplicationManager
import vladsaif.syncedit.plugin.audioview.waveform.EditionModel
import vladsaif.syncedit.plugin.audioview.waveform.EditionModel.EditionType.*
import vladsaif.syncedit.plugin.audioview.waveform.Player
import vladsaif.syncedit.plugin.modFloor
import vladsaif.syncedit.plugin.toDecodeFormat
import java.nio.file.Path
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.math.min

class PlayerImpl(private val file: Path) : Player {
    private val source: SourceDataLine
    private var processUpdater: (Long) -> Unit = {}
    private var signalStopReceived = false

    init {
        val fileFormat = AudioSystem.getAudioFileFormat(file.toFile())
        source = AudioSystem.getSourceDataLine(fileFormat.format.toDecodeFormat())
    }

    override fun applyEditions(editionModel: EditionModel) {
        AudioSystem.getAudioInputStream(file.toFile()).use {
            val inputStream = it
            AudioSystem.getAudioInputStream(inputStream.format.toDecodeFormat(), inputStream).use {
                applyEditionImpl(it, editionModel)
            }
        }
    }

    private fun applyEditionImpl(decodedStream: AudioInputStream, editionModel: EditionModel) {
        val editions = editionModel.editions
        if (!source.isOpen) source.open(decodedStream.format)
        ApplicationManager.getApplication().invokeLater { source.start() }
        val frameSize = decodedStream.format.frameSize
        val buffer = ByteArray(1 shl 14)
        var totalFrames = 0L
        println(editions)
        outer@ for (edition in editions) {
            var needBytes = edition.first.length * frameSize
            when (edition.second) {
                CUT -> {
                    totalFrames += needBytes / frameSize
                    processUpdater(totalFrames)
                    while (needBytes != 0L && !signalStopReceived) {
                        val skipped = decodedStream.skip(needBytes)
                        needBytes -= skipped
                        if (skipped == 0L || signalStopReceived) {
                            break@outer
                        }
                    }
                }
                MUTE -> {
                    buffer.fill(0)
                    var needSkip = needBytes
                    while (needBytes != 0L || needSkip != 0L) {
                        if (needBytes != 0L) {
                            val zeroesCount = min(buffer.size.toLong(), needBytes)
                                    .toInt()
                                    .modFloor(frameSize)
                            if (signalStopReceived) {
                                break@outer
                            }
                            writeOrBlock(buffer, zeroesCount)
                            totalFrames += zeroesCount / frameSize
                            processUpdater(totalFrames)
                            needBytes -= zeroesCount
                        }
                        if (needSkip != 0L) {
                            val skipped = decodedStream.skip(needSkip)
                            needSkip -= skipped
                            if (skipped == 0L) {
                                break@outer
                            }
                        }
                    }
                }
                NO_CHANGES -> {
                    while (needBytes != 0L) {
                        val read = decodedStream.read(buffer, 0, min(buffer.size.toLong(), needBytes).toInt())
                        if (read == -1 || signalStopReceived) {
                            println("Break no changes $signalStopReceived")
                            break@outer
                        }
                        needBytes -= read
                        writeOrBlock(buffer, read)
                        totalFrames += read / frameSize
                        processUpdater(totalFrames)
                    }
                }
            }
        }
        println(totalFrames / decodedStream.format.frameRate)
    }

    private fun writeOrBlock(buffer: ByteArray, size: Int) {
        var needWrite = size
        while (needWrite != 0) {
            val written = source.write(buffer, size - needWrite, needWrite)
            needWrite -= written
        }
    }

    /**
     * Set [updater] that will be sometimes called with the number of frames written to the source data line.
     */
    override fun setProcessUpdater(updater: (Long) -> Unit) {
        processUpdater = updater
    }

    override fun pause() {
        source.stop()
    }

    override fun stop() {
        signalStopReceived = true
        source.stop()
        source.drain()
        source.flush()
    }

    override fun close() {
        try {
            source.close()
        } catch (ex: Throwable) {
        }
    }

    override fun play() {
        source.start()
    }
}