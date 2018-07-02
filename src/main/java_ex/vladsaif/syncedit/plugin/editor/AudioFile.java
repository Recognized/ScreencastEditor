package vladsaif.syncedit.plugin.editor;

import javax.sound.sampled.*;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class AudioFile implements EditionListener {
    private final Path myFile;
    private final EventGroup myEventGroup;

    public AudioFile(Path file, EventGroup group) {
        myFile = file.toAbsolutePath();
        myEventGroup = group;
    }

    @Override
    public void onSomethingChanged() {
        try {
            saveChanges();
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    @Override
    public ListenerType getType() {
        return ListenerType.AUDIO;
    }

    private void saveChanges() throws IOException {
        Path tempOutput = Files.createTempFile(myFile.toAbsolutePath().getParent(), "__temp_", "_");
        try {
            copyEditedFile(tempOutput, myEventGroup.getTimeline().getDeletedRanges());
            try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(myFile))) {
                Files.copy(tempOutput, outputStream);
            }
        } catch (UnsupportedAudioFileException | IllegalArgumentException | SecurityException ex) {
            throw new IOException(ex);
        } finally {
            try {
                Files.delete(tempOutput);
            } catch (IOException ex) {
                System.err.println("Error during deleting " + tempOutput);
            }
        }
        System.out.println("Changes saved to " + myFile);
    }

    private void copyEditedFile(Path destination, List<TimeRange> deletedRanges) throws IOException, UnsupportedAudioFileException {
        AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(myFile.toFile());
        AudioFormat format = fileFormat.getFormat();
        float rate = format.getFrameRate();
        long frameSize = format.getFrameSize();
        try (AudioInputStream inputStream = AudioSystem.getAudioInputStream(myFile.toFile());
             OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(destination))) {
            long totalFrames = inputStream.getFrameLength();
            long prevEndFrame = 0;
            for (TimeRange range : deletedRanges) {
                long delStartFrame = Math.min((long) (range.getStartOffsetMs() * rate / 1000), totalFrames);
                long delEndFrame = Math.min((long) (range.getEndOffsetMs() * rate / 1000), totalFrames);
                long framesToWrite = delStartFrame - prevEndFrame;
                if (framesToWrite != 0) {
                    AudioInputStream toWrite = new AudioInputStream(inputStream, fileFormat.getFormat(), framesToWrite);
                    AudioSystem.write(toWrite, fileFormat.getType(), outputStream);
                }
                long totalSkip = (delEndFrame - delStartFrame) * frameSize;
                while (totalSkip != 0) {
                    long skipped = inputStream.skip(totalSkip);
                    totalSkip -= skipped;
                }
                prevEndFrame = delEndFrame;
                if (delEndFrame == totalFrames) break;
            }
            if (prevEndFrame != totalFrames) {
                AudioInputStream toWrite = new AudioInputStream(inputStream, fileFormat.getFormat(), totalFrames - prevEndFrame);
                AudioSystem.write(toWrite, fileFormat.getType(), outputStream);
            }
        }
    }
}
