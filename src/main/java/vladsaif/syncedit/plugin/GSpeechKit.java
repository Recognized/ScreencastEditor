package vladsaif.syncedit.plugin;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1p1beta1.*;
import com.google.protobuf.ByteString;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("unused")
public class GSpeechKit extends SpeechClient {

    public GSpeechKit(InputStream stream) throws IOException {
        super(getSettingsX(stream));
    }

    private static SpeechSettings getSettingsX(InputStream stream) throws IOException {
        return SpeechSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(ServiceAccountCredentials.fromStream(stream)))
                .build();
    }

    public static void checkCredentials(InputStream stream) throws IOException {
        getSettingsX(stream);
    }

    private static List<List<Object>> getData(LongRunningRecognizeResponse response) {
        List<List<Object>> data = new ArrayList<>();
        for (WordInfo each : response.getResults(0).getAlternatives(0).getWordsList()) {
            ArrayList<Object> inner = new ArrayList<>();
            inner.add(each.getWord());
            inner.add((int) (each.getStartTime().getSeconds() * 1000 + each.getStartTime().getNanos() / 1_000_000));
            inner.add((int) (each.getEndTime().getSeconds() * 1000 + each.getEndTime().getNanos() / 1_000_000));
            data.add(inner);
        }
        return data;
    }

    private static String toJson(LongRunningRecognizeResponse response) {
        return response.toString();
    }

    private static <T> CompletableFuture<T> buildCompletableFuture(
            final ApiFuture<T> listenableFuture
    ) {
        //create an instance of CompletableFuture
        CompletableFuture<T> completable = new CompletableFuture<T>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                // propagate cancel to the listenable future
                boolean result = listenableFuture.cancel(mayInterruptIfRunning);
                super.cancel(mayInterruptIfRunning);
                return result;
            }
        };

        // add callback
        ApiFutures.addCallback(listenableFuture, new ApiFutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                completable.complete(result);
            }

            @Override
            public void onFailure(Throwable t) {
                completable.completeExceptionally(t);
            }
        });
        return completable;
    }

    public List<List<Object>> recognize(InputStream inputStream) throws IOException, ExecutionException, InterruptedException {
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
        return getData(longRunningRecognizeAsync(config, audio).get());
    }
}