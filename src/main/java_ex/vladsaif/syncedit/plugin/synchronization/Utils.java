package vladsaif.syncedit.plugin.synchronization;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import vladsaif.syncedit.plugin.editor.TimeRange;
import vladsaif.syncedit.plugin.lang.dsl.DslFileType;
import vladsaif.syncedit.plugin.lang.transcript.TranscriptFileType;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static vladsaif.syncedit.plugin.editor.EditionListener.ListenerType;

public class Utils {
    private static final Comparator<Edition> ascendingRangeCmp = Comparator
            .comparingInt((Edition x) -> x.isInsertion() ? x.getInsertPosition() : x.getRange().getStartOffset());
    private static final Logger LOG = Logger.getInstance(Utils.class);

    private Utils() {
    }

    public static int parseTimeOffset(String s) {
        return Integer.parseInt(s);
    }

    public static String formatTimeRange(TimeRange range) {
        return "[" + range.getStartOffsetMs() + ", " + range.getEndOffsetMs() + "]";
    }

    public static ListenerType getByFileType(FileType fileType) {
        return fileType instanceof DslFileType ? ListenerType.SCRIPT
                : ((fileType instanceof TranscriptFileType) ? ListenerType.TRANSCRIPT : null);
    }

    public static FileType getByListenerType(ListenerType type) {
        return type.equals(ListenerType.TRANSCRIPT) ? TranscriptFileType.getInstance()
                : (type.equals(ListenerType.SCRIPT) ? DslFileType.getInstance() : null);
    }

    public static boolean isSynchronizableFileType(FileType x) {
        return x instanceof TranscriptFileType || x instanceof DslFileType;
    }

    public static String formatTimeRange(int timeOffset) {
        return "" + timeOffset;
    }

    public static int parseId(String s) {
        return Integer.parseInt(s);
    }

    public static boolean hasInvalidDescendant(@NotNull PsiElement start) {
        if (!start.isValid()) return true;
        for (PsiElement child = start.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (hasInvalidDescendant(child)) return true;
        }
        return false;
    }

    public static void processEditions(Project project, List<Edition> editions, @NotNull Document validDoc) {
        editions.sort(ascendingRangeCmp);
        LOG.info(toString(editions));
        TextRange deleted = null;
        int accumulator = 0;
        for (Edition ed : editions) {
            if (ed.isInsertion()) {
                validDoc.insertString(ed.getInsertPosition() + accumulator, ed.getText());
                accumulator += ed.getText().length();
            } else if (ed.isDeletion() && (deleted == null || !deleted.intersects(ed.getRange()))) {
                TextRange range = ed.getRange();
                validDoc.deleteString(range.getStartOffset() + accumulator, range.getEndOffset() + accumulator);
                accumulator -= range.getLength();
                if (deleted == null) {
                    deleted = range;
                } else {
                    deleted = new TextRange(Math.min(range.getStartOffset(), deleted.getStartOffset()),
                            Math.max(range.getEndOffset(), deleted.getEndOffset()));
                }
            } else if (ed.isReplacement()) {
                TextRange range = ed.getRange();
                validDoc.replaceString(range.getStartOffset() + accumulator, range.getEndOffset() + accumulator, ed.getText());
                accumulator = accumulator + ed.getText().length() - range.getLength();
            }
        }
    }

    /**
     * Deletes specified segments of text in synchronized file.
     *
     * @param doc        affected document.
     * @param textRanges sorted ranges to delete.
     * @return <tt>true</tt> if something was actually deleted, <tt>false</tt> otherwise.
     */
    public static boolean deleteTextRange(Document doc, List<TextRange> textRanges) {
        boolean changed = false;
        int offsetAccumulator = 0;
        for (TextRange del : textRanges) {
            doc.deleteString(del.getStartOffset() - offsetAccumulator, del.getEndOffset() - offsetAccumulator);
            offsetAccumulator += del.getLength();
            changed = true;
        }
        return changed;
    }

    public static String toString(Collection<?> container) {
        return container.stream()
                .map(Object::toString)
                .collect(Collectors.joining(",", "[", "]"));
    }

    public static String formatId(int id) {
        return "[" + id + "]";
    }
}
