package vladsaif.syncedit.plugin.speechrecognition;

import org.junit.Test;
import vladsaif.syncedit.plugin.editor.TimeRange;
import vladsaif.syncedit.plugin.speechrecognition.recognizers.GoogleSpeechKit;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class GoogleSpeechKitTest {
    private Path resources = Paths.get("src", "test", "resources");

    private Transcript testAudio(InputStream audio) throws Exception {
        InputStream credentialsStream = Files.newInputStream(resources.resolve("google_key.json"));
        try (GoogleSpeechKit recognizer = new GoogleSpeechKit(credentialsStream)) {
            return recognizer.recognize(audio);
        }
    }

    @Test
    public void test01_WAV_audio() throws Exception {
        Transcript data = testAudio(Files.newInputStream(resources.resolve("sample.wav")));
        System.out.println(data.getText());
        for (Map.Entry<TimeRange, String> word : data.getWords().entrySet()) {
            System.out.println(word.getValue() + " " + word.getKey());
        }
    }
}
