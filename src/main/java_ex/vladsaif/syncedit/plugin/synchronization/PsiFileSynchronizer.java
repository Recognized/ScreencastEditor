package vladsaif.syncedit.plugin.synchronization;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import vladsaif.syncedit.plugin.editor.EditionListener;
import vladsaif.syncedit.plugin.editor.EventGroup;
import vladsaif.syncedit.plugin.editor.Timeline;
import vladsaif.syncedit.plugin.lang.dsl.DslFileType;

import java.util.*;
import java.util.stream.Collectors;

public abstract class PsiFileSynchronizer<T extends Statement> implements EditionListener {
    private static final Logger LOG = Logger.getInstance(PsiFileSynchronizer.class);
    protected final Map<Integer, T> myOriginalStatements = ContainerUtil.newHashMap();
    protected final Map<Integer, T> myCurrentStatements = new HashMap<>();
    protected final EventGroup myEventGroup;
    protected final String myUrl;
    private final String myName;
    private final List<T> sortedOriginalStatements = new ArrayList<>();
    private final Project myProject;
    private long myModStamp;
    private boolean initialReadSucceeded = false;
    private boolean killed = false;
    private PsiFile myPsiFile;

    public static PsiFileSynchronizer create(FileType fileType, PsiFile file, EventGroup group) {
        if (!Utils.isSynchronizableFileType(fileType)) {
            throw new IllegalArgumentException();
        }
        return fileType instanceof DslFileType ? new DslSynchronizer(file, group) : new TranscriptSynchronizer(file, group);
    }

    PsiFileSynchronizer(@NotNull PsiFile file, EventGroup eventGroup) {
        PsiUtilCore.ensureValid(file);
        myEventGroup = eventGroup;
        myName = file.getName();
        myProject = file.getProject();
        myPsiFile = file;
        myUrl = file.getVirtualFile() != null ? file.getVirtualFile().getUrl() : "";
        myModStamp = myPsiFile.getModificationStamp();
    }

    @Override
    public void onSomethingChanged() {
        if (killed) return;
        TransactionGuard.getInstance().assertWriteSafeContext(ModalityState.current());
        Document doc = getDocument();
        if (hasErrors() || doc == null) {
            return;
        }
        List<T> added = collectStatements();
        if (isValidStatements(added)) {
            SynchronizationManager manager = SynchronizationManager.getInstance();
            manager.addAffectedDocument(doc);
            List<T> oldStatements = copyCurrentStatements();
            List<T> newCurrentStatements = updateFile();
            manager.addUndoAction(() -> setCurrentStatements(oldStatements));
            manager.addRedoAction(() -> setCurrentStatements(newCurrentStatements));
            setCurrentStatements(newCurrentStatements);
        }
        updateStamp();
    }

    /**
     * Obtaining changes in the file and notifying files linked with it.
     * If the file has errors, synchronization for this file cannot be performed.
     */
    public void obtainChanges() {
        if (killed) return;
        TransactionGuard.getInstance().assertWriteSafeContext(ModalityState.current());
        updateStamp();
        Document doc = getDocument();
        if (hasErrors() || doc == null || !PsiDocumentManager.getInstance(getProject()).isCommitted(doc)) {
            LOG.info("Cannot modify file");
            return;
        }
        List<T> freshStatements = collectStatements();
        if (isValidStatements(freshStatements)) {
            Timeline oldTimeline = myEventGroup.getTimeline().copy();
            List<T> oldCurrentStatements = copyCurrentStatements();
            UpdateResult result = updateModel(freshStatements);
            if (result.equals(UpdateResult.FAILED)) {
                LOG.info("File " + myName + " has errors.");
                return;
            }
            freshStatements.forEach(x -> myOriginalStatements.get(x.getId()).setWord(x.getWord()));
            setCurrentStatements(freshStatements);
            if (result.equals(UpdateResult.UPDATED)) {
                Timeline currentTimeline = myEventGroup.getTimeline().copy();
                List<T> newCurrentStatements = copyCurrentStatements();
                SynchronizationManager manager = SynchronizationManager.getInstance();
                manager.addUndoAction(() -> rollback(oldTimeline, oldCurrentStatements));
                manager.addRedoAction(() -> rollback(currentTimeline, newCurrentStatements));
                manager.setUndoFinishAction(this::finishUndo);
                manager.addAffectedDocument(doc);
                notifySomethingChanged();
                manager.undoableActionPerformed(getProject());
            }
        }
        updateStamp();
    }

    /**
     * Called on the first read of the file.
     * If the file has errors then initial read for this file will be performed when file changes.
     */
    public void initialRead() {
        if (!myPsiFile.isValid()) {
            LOG.error("File is not valid anymore");
            return;
        }
        List<T> statements = collectStatements();
        if (statements == null) {
            LOG.error("Invalid file content.");
            return;
        }
        statements.forEach(x -> myOriginalStatements.put(x.getId(), x));
        setCurrentStatements(statements);
        sortedOriginalStatements.addAll(myOriginalStatements.values());
        sortedOriginalStatements.sort(Comparator.comparingInt(Statement::getId));
        initialReadSucceeded = true;
        updateStamp();
    }

    public Project getProject() {
        return myProject;
    }

    @NotNull
    public PsiFile getPsiFile() {
        return myPsiFile;
    }

    public void updatePsiFile() {
        VirtualFile fileByUrl = VirtualFileManager.getInstance().findFileByUrl(myUrl);
        if (fileByUrl != null) {
            PsiFile newPsiFile = PsiManager.getInstance(getProject()).findFile(fileByUrl);
            if (newPsiFile != null) {
                myPsiFile = newPsiFile;
            }
        }
    }

    /**
     * Returns whether this synchronizer needs update or not.
     *
     * @return <tt>true</tt> if some changes were made to file since last read,
     * <tt>false</tt> otherwise.
     */
    public boolean needsUpdate() {
        return myModStamp != myPsiFile.getModificationStamp();
    }

    public boolean isValid() {
        return myPsiFile.isValid() && !killed;
    }

    public void shutdown() {
        killed = true;
        myEventGroup.removeListener(this);
    }

    @Override
    public String toString() {
        return "PsiFileSynchronizer{" +
                "myEventGroup=" + myEventGroup +
                ", myName=" + myName +
                ", stamp=" + myModStamp +
                '}';
    }


    public enum UpdateResult {
        UPDATED, FAILED, NOT_UPDATED;
    }

    /**
     * Returns whether {@link #initialRead()} had been successfully finished.
     *
     * @return <tt>true</tt>, if finished successfully, <tt>false</tt>, otherwise.
     */
    public boolean needRunInitialRead() {
        return !initialReadSucceeded;
    }

    protected Document getDocument() {
        return PsiDocumentManager.getInstance(getProject()).getDocument(myPsiFile);
    }

    /**
     * Sets <tt>statements</tt> to be {@link #myCurrentStatements}.
     *
     * @param statements to set.
     */
    private void setCurrentStatements(List<T> statements) {
        myCurrentStatements.clear();
        statements.forEach(x -> myCurrentStatements.put(x.getId(), x));
    }

    /**
     * Returns copy of currents statements.
     *
     * @return such copy.
     */
    private List<T> copyCurrentStatements() {
        return myCurrentStatements.entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    /**
     * Checks whether file has invalid PSI or error elements.
     *
     * @return <tt>true</tt> if it has, <tt>false</tt> otherwise.
     */
    private boolean hasErrors() {
        if (PsiUtilCore.hasErrorElementChild(myPsiFile) || Utils.hasInvalidDescendant(myPsiFile)) {
            LOG.info("File is not valid: " + myName);
            return true;
        }
        return false;
    }

    /**
     * Synchronizes file content with timeline.
     *
     * @return sorted list of statements in updated file.
     */
    protected abstract List<T> updateFile();

    /**
     * Returns original statements sorted by id in ascending order.
     *
     * @return this statements.
     */
    protected List<T> getSortedOriginalStatements() {
        return Collections.unmodifiableList(sortedOriginalStatements);
    }

    /**
     * Updates {@link #myModStamp}.
     */
    private void updateStamp() {
        myModStamp = myPsiFile.getModificationStamp();
    }

    /**
     * Last action that is made after undo was requested by user.
     */
    private void finishUndo() {
        UndoManager instance = UndoManager.getInstance(getProject());
        ApplicationManager.getApplication().invokeLater(() -> {
            synchronized (PsiFileSynchronizer.this) {
                FileEditor editor = FileEditorManager.getInstance(getProject()).getSelectedEditor(myPsiFile.getVirtualFile());
                if (editor == null) return;
                if (instance.isUndoAvailable(editor)) {
                    instance.undo(editor);
                }
            }
        });
    }

    /**
     * Sets timeline {@link EventGroup#getTimeline()} and {@link #myCurrentStatements} to the given.
     *
     * @param currentTimeline      timeline to set.
     * @param newCurrentStatements statements to set.
     */
    private void rollback(Timeline currentTimeline, List<T> newCurrentStatements) {
        myEventGroup.getTimeline().load(currentTimeline);
        setCurrentStatements(newCurrentStatements);
    }

    /**
     * Updates model {@link Timeline} (and maybe file) reading changes made to file.
     *
     * @param freshStatements current statements retrieved from file.
     * @return <tt>UpdateResult.FAILED</tt> if file has unexpected changes, that cannot be interpreted as model change.
     * <tt>UpdateResult.NOT_UPDATED</tt> if model was not updated.
     * <tt>UpdateResult.UPDATED</tt> if some changes were made to timeline.
     * @see EventGroup#getTimeline()
     */
    protected abstract UpdateResult updateModel(List<T> freshStatements);

    /**
     * Checks if <tt>statements</tt> have ids of original statements.
     *
     * @param statements to check.
     * @return <tt>true</tt>, if <tt>statements</tt> not null and have ids of original statements,
     * <tt>false</tt> otherwise.
     */
    @Contract("null -> false")
    protected boolean isValidStatements(@Nullable List<T> statements) {
        if (statements == null) return false;
        return statements.size() <= myOriginalStatements.size() && statements.stream()
                .allMatch(x -> myOriginalStatements.get(x.getId()) != null);
    }

    /**
     * Notify that {@link Timeline} was changed by this synchronizer.
     *
     * @see EventGroup#getTimeline()
     */
    protected abstract void notifySomethingChanged();

    /**
     * Iterate over statements in corresponding file and add them sequentially to returned list.
     *
     * @return <tt>null</tt> if statements in file are not correctly ordered, otherwise,
     * list of statements in file in the order they appear.
     */
    protected abstract @Nullable
    List<T> collectStatements();
}
