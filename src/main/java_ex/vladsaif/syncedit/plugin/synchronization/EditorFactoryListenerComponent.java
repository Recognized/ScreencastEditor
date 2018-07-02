package vladsaif.syncedit.plugin.synchronization;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class EditorFactoryListenerComponent implements ProjectComponent {

    private EditorFactoryListenerComponent(EditorFactory factory) {
        factory.addEditorFactoryListener(new EditorFactoryListener() {
            @Override
            public void editorCreated(@NotNull EditorFactoryEvent event) {
                final Project project = event.getEditor().getProject();
                if (project != null) {
                    final Document doc = event.getEditor().getDocument();
                    final PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(doc);
                    if (file != null && Utils.isSynchronizableFileType(file.getFileType())) {
                        SynchronizationManager.getInstance().fileOpened(file);
                    }
                }
            }

            @Override
            public void editorReleased(@NotNull EditorFactoryEvent event) {
                final Project project = event.getEditor().getProject();
                if (project != null) {
                    final Document doc = event.getEditor().getDocument();
                    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(doc);
                    if (file != null && Utils.isSynchronizableFileType(file.getFileType())) {
                        SynchronizationManager.getInstance().fileClosed(file);
                    }
                }
            }
        }, ApplicationManager.getApplication());
    }
}
