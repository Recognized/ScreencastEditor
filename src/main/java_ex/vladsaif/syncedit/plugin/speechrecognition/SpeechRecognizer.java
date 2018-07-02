package vladsaif.syncedit.plugin.speechrecognition;

import vladsaif.syncedit.plugin.speechrecognition.recognizers.GoogleSpeechKit;

import java.io.IOException;
import java.io.InputStream;

public interface SpeechRecognizer {

    Transcript recognize(InputStream inputStream) throws IOException;

    static SpeechRecognizer getDefault(InputStream credentials) throws IOException {
        return new GoogleSpeechKit(credentials);
    }
}
