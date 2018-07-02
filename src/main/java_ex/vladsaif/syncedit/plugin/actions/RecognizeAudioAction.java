package vladsaif.syncedit.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.vfs.VirtualFile;
import vladsaif.syncedit.plugin.editor.EventGroupManager;
import vladsaif.syncedit.plugin.editor.TimeRange;
import vladsaif.syncedit.plugin.synchronization.Utils;
import vladsaif.syncedit.plugin.speechrecognition.SpeechRecognizer;
import vladsaif.syncedit.plugin.speechrecognition.Transcript;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class RecognizeAudioAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(RecognizeAudioAction.class);

    @Override
    public void actionPerformed(AnActionEvent e) {
        FileChooserDescriptor chooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor();
        FileChooser.chooseFile(chooserDescriptor, e.getProject(), null, (x) -> ApplicationManager.getApplication().executeOnPooledThread(() -> fileChosen(x)));
    }

    private void fileChosen(VirtualFile file) {
        try {
            Path audioPath = new File(file.getPath()).toPath().toAbsolutePath();
            try (InputStream is = Files.newInputStream(Paths.get("C:", "key.json"))) {
                SpeechRecognizer speechKit = SpeechRecognizer.getDefault(is);
                Transcript transcript = speechKit.recognize(Files.newInputStream(audioPath));
                String fileName = EventGroupManager.getFileNameWithoutExtension(audioPath) + ".transcript";
                try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(audioPath.resolveSibling(fileName))))) {
                    int counter = 1;
                    for (Map.Entry<TimeRange, String> word : transcript.getWords().entrySet()) {
                        writer.write(word.getValue());
                        writer.write(" ");
                        writer.write(Utils.formatTimeRange(word.getKey()));
                        writer.write(" ");
                        writer.write(Utils.formatId(counter++));
                        writer.write("\n");
                    }
                }
                LOG.info("Transcript is ready.");
            } catch (IOException ex) {
                LOG.error(ex);
            }
        } catch (Throwable throwable) {
            LOG.error(throwable);
        }
    }
}
