package vladsaif.syncedit.plugin;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.WriteChannel;
import com.google.cloud.speech.v1p1beta1.*;
import com.google.cloud.storage.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SuppressWarnings("unused")
public class GSpeechKitInternal extends SpeechClient {

    public GSpeechKitInternal(InputStream stream) throws IOException {
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
        if (response.getResultsList().isEmpty() || response.getResults(0).getAlternativesList().isEmpty()) {
            throw new RuntimeException("Recognition result is empty.");
        }
        List<List<Object>> data = new ArrayList<>();
        for (SpeechRecognitionResult result : response.getResultsList()) {
            for (WordInfo each : result.getAlternatives(0).getWordsList()) {
                ArrayList<Object> inner = new ArrayList<>();
                inner.add(each.getWord());
                inner.add((int) (each.getStartTime().getSeconds() * 1000 + each.getStartTime().getNanos() / 1_000_000));
                inner.add((int) (each.getEndTime().getSeconds() * 1000 + each.getEndTime().getNanos() / 1_000_000));
                data.add(inner);
            }
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

    private static final String BUCKET_NAME = "screencast_editor_temporary_audio_storage";

    private static String getBlobUri(String fileName) {
        return "gs://" + BUCKET_NAME + "/" + fileName;
    }

    private void upload(String fileName, InputStream inputStream) throws IOException {
        Storage storage = StorageOptions.newBuilder()
                .setCredentials(getSettings().getCredentialsProvider().getCredentials())
                .build()
                .getService();
        try {
            storage.create(BucketInfo.of(BUCKET_NAME));
        } catch (StorageException ex) {
            if (ex.getCode() != 409) {
                throw ex;
            }
            // "You already own this bucket"
        }
        BlobId blobId = BlobId.of(BUCKET_NAME, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("audio/wav").build();
        Blob blob = storage.create(blobInfo);
        try (WriteChannel writer = blob.writer()) {
            ByteBuffer buffer = ByteBuffer.allocate(1 << 14);
            byte[] array = buffer.array();
            while (true) {
                int bytesRead = inputStream.read(array);
                if (bytesRead == -1) {
                    break;
                }
                if (bytesRead == 0) {
                    continue;
                }
                buffer.limit(bytesRead);
                while (buffer.hasRemaining()) {
                    writer.write(buffer);
                }
                buffer.position(0);
            }
        }

    }

    private void delete(String fileName) {
        try {
            Storage storage = StorageOptions.newBuilder()
                    .setCredentials(getSettings().getCredentialsProvider().getCredentials())
                    .build()
                    .getService();
            storage.delete(BlobId.of(BUCKET_NAME, fileName));
        } catch (IOException ex) {
            throw new RuntimeException("Failed to delete temp file at " + getBlobUri(fileName));
        }
    }

    public CompletableFuture<List<List<Object>>> recognize(InputStream inputStream) throws IOException {
        String fileName = "temp_file" + System.currentTimeMillis();
        try {
            upload(fileName, inputStream);
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEnableWordTimeOffsets(true)
                    .setModel("video")
                    .setLanguageCode("en-US")
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(44100)
                    .setEnableAutomaticPunctuation(true)
                    .build();
            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setUri(getBlobUri(fileName))
                    .build();
            Executor ex = Executors.newSingleThreadExecutor();
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return getData(longRunningRecognizeAsync(config, audio).get());
                } catch (ExecutionException | InterruptedException exception) {
                    throw new RuntimeException(exception);
                } finally {
                    delete(fileName);
                }
            }, ex);
        } catch (Throwable ex) {
            // Delete on fail
            delete(fileName);
            throw ex;
        }
    }

}