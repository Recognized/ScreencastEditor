package vladsaif.syncedit.plugin.synchronization.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.command.undo.DocumentReference;
import com.intellij.openapi.command.undo.DocumentReferenceManager;
import com.intellij.openapi.command.undo.UndoManager;
import com.intellij.openapi.command.undo.UndoableAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import vladsaif.syncedit.plugin.editor.*;
import vladsaif.syncedit.plugin.editor.EditionListener.ListenerType;
import vladsaif.syncedit.plugin.synchronization.PsiFileSynchronizer;
import vladsaif.syncedit.plugin.synchronization.SynchronizationManager;
import vladsaif.syncedit.plugin.synchronization.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

public class SynchronizationManagerImpl implements SynchronizationManager {
    private static final Logger LOG = Logger.getInstance(SynchronizationManagerImpl.class);
    private final Map<PsiFile, PsiFileSynchronizer> activeSynchronizers = ContainerUtil.newConcurrentMap();
    private final Set<Path> loadedMappingFiles = new HashSet<>();
    private RefreshLoop refreshLoop;

    private MyUndoableAction currentUndoableAction;

    @Override
    public Set<PsiFileSynchronizer> getActiveSynchronizers() {
        return activeSynchronizers.entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
    }

    @Override
    public void setUndoFinishAction(Runnable finishUndo) {
        currentUndoableAction.undoLast = finishUndo;
    }

    @Override
    public void addUndoAction(Runnable undo) {
        if (currentUndoableAction == null) {
            currentUndoableAction = new MyUndoableAction();
        }
        currentUndoableAction.undoAction.add(undo);
    }

    @Override
    public void addRedoAction(Runnable redo) {
        if (currentUndoableAction == null) {
            currentUndoableAction = new MyUndoableAction();
        }
        currentUndoableAction.redoAction.add(redo);
    }

    @Override
    public void undoableActionPerformed(Project project) {
        if (currentUndoableAction != null) {
            UndoManager.getInstance(project).undoableActionPerformed(currentUndoableAction);
            currentUndoableAction = null;
        }
    }

    @Override
    public void addAffectedDocument(Document doc) {
        if (currentUndoableAction != null) {
            currentUndoableAction.references.add(DocumentReferenceManager.getInstance().create(doc));
        }
    }

    @Override
    public void fileClosed(PsiFile file) {
        PsiFileSynchronizer synchronizer = activeSynchronizers.get(file);
        if (synchronizer != null) {
            trySynchronize(synchronizer);
            activeSynchronizers.remove(file);
            if (activeSynchronizers.isEmpty()) {
                LOG.info("Refresh loop disabled.");
                refreshLoop.isRefreshLoopActive = false;
                refreshLoop = null;
            }
        }
    }

    @Override
    public void fileOpened(PsiFile file) {
        System.out.println(file.getVirtualFile().getUrl());
        if (!file.isValid()) {
            LOG.info("Opened invalid file " + file);
            return;
        }
        if (Utils.isSynchronizableFileType(file.getFileType())) {
            VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile == null) {
                return;
            }
            Path filePath = new File(virtualFile.getPath()).toPath().toAbsolutePath();
            if (loadedMappingFiles.add(getDefaultMappingFile(filePath))) {
                Map<Path, ListenerType> mapping = getDefaultMapping(file.getProject(), filePath);
                System.out.println(mapping);
                if (mapping != null) {
                    try {
                        EventGroupManager.getInstance().loadMapping(mapping.entrySet()
                                .stream()
                                .map(Map.Entry::getKey)
                                .collect(Collectors.toList()));
                    } catch (InvalidMapping ex) {
                        Messages.showErrorDialog(file.getProject(),
                                "Invalid mapping in file: " + getDefaultMappingFile(filePath) + ". "
                                        + ex.getMessage(), "SyncEdit Plugin");
                        return;
                    }
                    mapping.forEach((key, value) -> {
                        EventGroup group = EventGroupManager.getInstance().getGroup(key);
                        if (group.getListener(value) != null) return;
                        if (!value.equals(ListenerType.AUDIO)) {
                            VirtualFile source = VirtualFileManager.getInstance().findFileByUrl("file://" + key.toAbsolutePath());
                            System.out.println("source " + source);
                            if (source == null) {
                                return;
                            }
                            PsiFile newFile = PsiManager.getInstance(file.getProject()).findFile(source);
                            System.out.println("newfile " + newFile);
                            if (newFile == null || !newFile.isValid()) {
                                return;
                            }
                            PsiFileSynchronizer sync = PsiFileSynchronizer.create(Utils.getByListenerType(value),
                                    newFile, group);
                            System.out.println(sync);
                            group.addListener(sync);
                        } else {
                            group.addListener(new AudioFile(key, group));
                        }
                    });
                }
            }
            EventGroup group = getGroup(virtualFile);
            EditionListener listener = group.getListener(Utils.getByFileType(file.getFileType()));
            if (listener == null) {
                listener = PsiFileSynchronizer.create(file.getFileType(), file, group);
                group.addListener(listener);
            }
            if (listener instanceof PsiFileSynchronizer) {
                PsiFileSynchronizer synchronizer = (PsiFileSynchronizer) listener;
                activeSynchronizers.put(file, synchronizer);
                if (synchronizer.needRunInitialRead()) {
                    ApplicationManager.getApplication().runReadAction(synchronizer::initialRead);
                }
                if (refreshLoop == null) {
                    LOG.info("Refresh loop activated.");
                    refreshLoop = new RefreshLoop();
                    ApplicationManager.getApplication().executeOnPooledThread(refreshLoop);
                }
            }
        }
    }

    /**
     * Obtain changes in the file if it was modified.
     */
    private void trySynchronize(PsiFileSynchronizer sync) {
        if (sync.isValid()) {
            if (sync.needsUpdate()) {
                ApplicationManager.getApplication().invokeAndWait(() -> {
                    Project project = sync.getProject();
                    if (project.isDisposed()) {
                        return;
                    }
                    if (sync.needRunInitialRead()) {
                        ApplicationManager.getApplication().runReadAction(sync::initialRead);
                    }
                    System.out.println("Obtaining " + sync);
                    WriteCommandAction.runWriteCommandAction(project, sync::obtainChanges);
                });
                currentUndoableAction = null;
            }
        } else {
            ApplicationManager.getApplication().runReadAction(sync::updatePsiFile);
            // todo bug, not removing
            if (!sync.isValid()) {
                activeSynchronizers.remove(sync.getPsiFile());
                sync.shutdown();
                LOG.info(sync + " is not valid, removing from active list.");
            }
        }
    }

    private Path getDefaultMappingFile(Path file) {
        return new File(EventGroupManager.getFileNameWithoutExtension(file.toAbsolutePath()) + ".mapping").toPath();
    }

    private Map<Path, ListenerType> getDefaultMapping(Project project, Path file) {
        Path defaultMappingFile = getDefaultMappingFile(file);
        LOG.info("Getting default mapping file: " + defaultMappingFile);
        if (Files.notExists(defaultMappingFile)) {
            Messages.showWarningDialog(project, "No default mapping file was found for file " + file
                    + ". Using default map scheme.", "SyncEdit Plugin");
            return null;
        }
        try {
            return EventGroupManager.parseMappingFile(defaultMappingFile);
        } catch (ParseException ex) {
            Messages.showErrorDialog(project, ex.getMessage(), "SyncEdit Plugin");
        } catch (IOException ex) {
            Messages.showErrorDialog(project, "Unexpected error occurred during reading default mapping file: "
                    + defaultMappingFile + ". " + ex.getMessage(), "SyncEdit Plugin");
        }
        return null;
    }

    private EventGroup getGroup(@NotNull VirtualFile file) {
        return EventGroupManager.getInstance().getGroup(new File(file.getPath()).toPath().toAbsolutePath());
    }

    private static class MyUndoableAction implements UndoableAction {
        final List<DocumentReference> references = new ArrayList<>();
        final List<Runnable> undoAction = new ArrayList<>();
        final List<Runnable> redoAction = new ArrayList<>();
        Runnable undoLast;

        @Override
        public void undo() {
            undoAction.forEach(Runnable::run);
            if (undoLast != null) {
                undoLast.run();
            }
        }

        @Override
        public void redo() {
            redoAction.forEach(Runnable::run);
        }

        @Override
        public DocumentReference[] getAffectedDocuments() {
            return references.toArray(new DocumentReference[0]);
        }

        @Override
        public boolean isGlobal() {
            return false;
        }

    }

    private class RefreshLoop implements Runnable {
        volatile boolean isRefreshLoopActive = true;

        @Override
        public void run() {
            activeSynchronizers.forEach((key, value) -> trySynchronize(value));
            if (isRefreshLoopActive) {
                ApplicationManager.getApplication().executeOnPooledThread(this);
            }
        }
    }
}