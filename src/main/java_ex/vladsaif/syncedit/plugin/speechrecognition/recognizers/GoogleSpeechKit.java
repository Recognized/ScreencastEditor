package vladsaif.syncedit.plugin.speechrecognition.recognizers;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1p1beta1.*;
import com.google.protobuf.ByteString;
import vladsaif.syncedit.plugin.editor.TimeRange;
import vladsaif.syncedit.plugin.speechrecognition.SpeechRecognizer;
import vladsaif.syncedit.plugin.speechrecognition.Transcript;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;

public class GoogleSpeechKit extends SpeechClient implements SpeechRecognizer {

    public GoogleSpeechKit(InputStream credentialStream) throws IOException {
        super(SpeechSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(ServiceAccountCredentials.fromStream(credentialStream)))
                .build());
    }

    @Override
    public Transcript recognize(InputStream inputStream) throws IOException {
        ByteString audioBytes = ByteString.readFrom(inputStream);
        RecognitionConfig config = RecognitionConfig.newBuilder()
                .setEnableWordTimeOffsets(true)
                .setModel("video")
                .setLanguageCode("en-US")
                .setEnableAutomaticPunctuation(true)
                .build();
        RecognitionAudio audio = RecognitionAudio.newBuilder()
                .setContent(audioBytes)
                .build();
        RecognizeResponse response = super.recognize(config, audio);
        Map<TimeRange, String> wordData = response.getResults(0)
                .getAlternatives(0)
                .getWordsList()
                .stream()
                .collect(Collectors.toMap(this::keyMapper, WordInfo::getWord));
        return new Transcript(wordData);
    }

    private TimeRange keyMapper(WordInfo info) {
        return new TimeRange((int) (info.getStartTime().getSeconds() * 1000 + info.getStartTime().getNanos() / 1_000_000),
                (int) (info.getEndTime().getSeconds() * 1000 + info.getEndTime().getNanos() / 1_000_000));
    }
}
