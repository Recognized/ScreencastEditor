package vladsaif.syncedit.plugin.synchronization;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import vladsaif.syncedit.plugin.editor.EventGroup;
import vladsaif.syncedit.plugin.editor.TimeRange;
import vladsaif.syncedit.plugin.editor.Timeline;
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptId;
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptLine;
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptTimeOffset;
import vladsaif.syncedit.plugin.lang.transcript.psi.TranscriptTokenTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TranscriptSynchronizer extends PsiFileSynchronizer<Statement> {
    private static final Logger LOG = Logger.getInstance(TranscriptSynchronizer.class);

    protected TranscriptSynchronizer(PsiFile file, EventGroup group) {
        super(file, group);
        LOG.info("Transcript synchronizer created: " + this);
    }

    @Override
    protected List<Statement> updateFile() {
        Document doc = getDocument();
        if (doc == null) {
            return null;
        }
        List<TranscriptLine> lines = PsiTreeUtil.getChildrenOfTypeAsList(getPsiFile(), TranscriptLine.class);
        List<Statement> statements = lines.stream()
                .map(this::parseStatement)
                .collect(Collectors.toList());
        int i = 0;
        int lastInsertPosition = 0;
        List<Edition> editions = new ArrayList<>();
        List<Statement> newStatements = new ArrayList<>();
        for (Statement original : getSortedOriginalStatements()) {
            TimeRange shouldBe = myEventGroup.getTimeline().impose(original.getTimeRange());
            if (i < statements.size() && original.getId() == statements.get(i).getId()) {
                if (!shouldBe.isValid()) {
                    editions.add(Edition.makeDeletion(lines.get(i).getTextRange()));
                } else if (!shouldBe.equals(statements.get(i).getTimeRange())) {
                    editions.add(Edition.makeReplacement(findTextRangeOfTimeRange(lines.get(i)), toString(shouldBe)));
                    newStatements.add(new Statement(statements.get(i).getWord(), shouldBe, original.getId()));
                } else {
                    newStatements.add(statements.get(i));
                }
                lastInsertPosition = lines.get(i).getTextRange().getEndOffset() + 1;
                ++i;
            } else if (shouldBe.isValid()) {
                Statement e = new Statement(original.getWord(), shouldBe, original.getId());
                editions.add(Edition.makeInsertion(lastInsertPosition, toString(e)));
                newStatements.add(e);
            }
        }
        Utils.processEditions(getProject(), editions, doc);
        return newStatements;
    }

    @Override
    protected @NotNull
    UpdateResult updateModel(List<Statement> freshStatements) {
        Timeline timeline = myEventGroup.getTimeline();
        List<Statement> currentStatements = myCurrentStatements.entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .sorted()
                .collect(Collectors.toList());
        int i = 0;
        Document doc = getDocument();
        if (doc == null) {
            LOG.info("Doc is null, update failed.");
            return UpdateResult.FAILED;
        }
        for (Statement cur : freshStatements) {
            Statement prev = myCurrentStatements.get(cur.getId());
            TimeRange shouldBe = myEventGroup.getTimeline().impose(myOriginalStatements.get(cur.getId()).getTimeRange());
            if (prev == null || !shouldBe.equals(cur.getTimeRange())) {
                // For now, lets consider only deletions as valid changes.
                LOG.info("Unexpected changes in file. Update failed.");
                return UpdateResult.FAILED;
            }
        }
        boolean changed = false;
        List<TranscriptLine> lines = PsiTreeUtil.getChildrenOfTypeAsList(getPsiFile(), TranscriptLine.class);
        List<Edition> editions = new ArrayList<>();
        for (Statement cur : currentStatements) {
            if (i >= freshStatements.size() || cur.getId() != freshStatements.get(i).getId()) {
                changed = true;
                timeline.delete(myOriginalStatements.get(cur.getId()).getTimeRange());
            } else {
                ++i;
            }
        }
        i = 0;
        for (Statement cur : currentStatements) {
            TimeRange shouldBe = timeline.impose(myOriginalStatements.get(cur.getId()).getTimeRange());
            if (!shouldBe.isValid()) {
                continue;
            }
            if (i < freshStatements.size() && cur.getId() == freshStatements.get(i).getId()) {
                if (!shouldBe.equals(freshStatements.get(i).getTimeRange())) {
                    editions.add(Edition.makeReplacement(findTextRangeOfTimeRange(lines.get(i)), toString(shouldBe)));
                    Statement is = freshStatements.get(i);
                    freshStatements.set(i, new Statement(is.getWord(), shouldBe, is.getId()));
                }
                ++i;
            }
        }
        if (editions.size() > 0) changed = true;
        Utils.processEditions(getProject(), editions, doc);
        return changed ? UpdateResult.UPDATED : UpdateResult.NOT_UPDATED;
    }

    private static TextRange findTextRangeOfTimeRange(TranscriptLine line) {
        int start = 0;
        for (PsiElement element = line.getFirstChild(); element != null; element = element.getNextSibling()) {
            if (element.getNode().getElementType().equals(TranscriptTokenTypes.OPEN_BRACKET)) {
                start = element.getTextRange().getStartOffset();
            } else if (element.getNode().getElementType().equals(TranscriptTokenTypes.CLOSE_BRACKET)) {
                return new TextRange(start, element.getTextRange().getEndOffset());
            }
        }
        throw new AssertionError();
    }

    private static String toString(Statement s) {
        return s.getWord() + " " + toString(s.getTimeRange()) + " [" + s.getId() + "]\n";
    }

    private static String toString(TimeRange range) {
        return "[" + range.getStartOffsetMs() + ", " + range.getEndOffsetMs() + "]";
    }

    @Override
    protected void notifySomethingChanged() {
        myEventGroup.notifyDsl();
        myEventGroup.notifyFiles();
    }

    @Override
    protected List<Statement> collectStatements() {
        List<TranscriptLine> lines = PsiTreeUtil.getChildrenOfTypeAsList(getPsiFile(), TranscriptLine.class);
        TranscriptOrderAdder adder = new TranscriptOrderAdder();
        for (TranscriptLine line : lines) {
            if (!adder.add(parseStatement(line))) {
                return null;
            }
        }
        return adder.getAdded();
    }

    private Statement parseStatement(TranscriptLine line) {
        int offsets[] = new int[2];
        int count = 0;
        int id = 0;
        String word = null;
        for (PsiElement x = line.getFirstChild(); x != null; x = x.getNextSibling()) {
            if (x instanceof TranscriptTimeOffset) {
                offsets[count++] = Utils.parseTimeOffset(x.getText());
            } else if (x instanceof TranscriptId) {
                id = Utils.parseId(x.getText());
            } else if (x.getNode().getElementType().equals(TranscriptTokenTypes.WORD)) {
                word = x.getText();
            }
        }
        return new Statement(word, new TimeRange(offsets[0], offsets[1]), id);
    }

    @Override
    public ListenerType getType() {
        return ListenerType.TRANSCRIPT;
    }

    @Override
    public String toString() {
        return "TranscriptSynchronizer{" + myUrl.substring(Math.max(myUrl.length() - 25, 0)) + "}";
    }

    /**
     * Checks that ranges are sorted and do not intersect.
     */
    private static class TranscriptOrderAdder extends OrderAdder<Statement> {
        private Statement lastAdded;

        @Override
        public boolean checkOrder(Statement word) {
            if (lastAdded == null) {
                lastAdded = word;
                return true;
            }
            boolean ret = word.getTimeRange().isValid()
                    && lastAdded.getTimeRange().getEndOffsetMs() <= word.getTimeRange().getStartOffsetMs()
                    && word.getId() > lastAdded.getId();
            lastAdded = word;
            return ret;
        }
    }
}
