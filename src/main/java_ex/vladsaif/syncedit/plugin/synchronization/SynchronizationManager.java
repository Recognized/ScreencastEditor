package vladsaif.syncedit.plugin.synchronization;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import java.util.Set;


public interface SynchronizationManager {

    static SynchronizationManager getInstance() {
        return ServiceManager.getService(SynchronizationManager.class);
    }

    void setUndoFinishAction(Runnable finishUndo);

    void addUndoAction(Runnable undo);

    void addRedoAction(Runnable redo);

    void undoableActionPerformed(Project project);

    void addAffectedDocument(Document doc);

    void fileClosed(PsiFile file);

    void fileOpened(PsiFile file);

    Set<PsiFileSynchronizer> getActiveSynchronizers();
}
