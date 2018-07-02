package vladsaif.syncedit.plugin.synchronization;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.IntIntHashMap;
import com.intellij.util.containers.Stack;
import vladsaif.syncedit.plugin.editor.EventGroup;
import vladsaif.syncedit.plugin.editor.TimeRange;
import vladsaif.syncedit.plugin.editor.Timeline;
import vladsaif.syncedit.plugin.lang.dsl.psi.DslId;
import vladsaif.syncedit.plugin.lang.dsl.psi.DslMetaComment;
import vladsaif.syncedit.plugin.lang.dsl.psi.DslStatement;
import vladsaif.syncedit.plugin.lang.dsl.psi.DslTimeOffset;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Manages synchronization of open in editor {@link vladsaif.syncedit.plugin.lang.dsl.psi.DslFile}
 */
public class DslSynchronizer extends PsiFileSynchronizer<EnclosingStatement> {
    private static final Logger LOG = Logger.getInstance(DslSynchronizer.class);
    private static final String UNIT_OFFSET = "    ";

    protected DslSynchronizer(PsiFile file, EventGroup group) {
        super(file, group);
        LOG.info("DslSynchronizer created: " + this);
    }

    /**
     * Parses time offset that is contained in comment.
     */
    private static int extractOffset(DslMetaComment comment) {
        PsiElement start = comment.getFirstChild();
        while (!(start instanceof DslTimeOffset)) {
            start = start.getNextSibling();
        }
        return Utils.parseTimeOffset(start.getText());
    }

    @Override
    protected void notifySomethingChanged() {
        myEventGroup.notifyFiles();
        myEventGroup.notifyTranscript();
    }

    @Override
    protected boolean isValidStatements(List<EnclosingStatement> statements) {
        if (!super.isValidStatements(statements)) {
            return false;
        }
        for (EnclosingStatement s : statements) {
            if (s.getDepth() != myOriginalStatements.get(s.getId()).getDepth()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected List<EnclosingStatement> updateFile() {
        Document doc = getDocument();
        if (doc == null) {
            return null;
        }
        List<DslStatement> elements = new ArrayList<>();
        List<EnclosingStatement> statements = new ArrayList<>();
        visitFile((element, statement) -> {
            elements.add(element);
            statements.add(statement);
        }, getPsiFile());
        int i = 0;
        IntIntHashMap depthToinsertPositions = new IntIntHashMap();
        depthToinsertPositions.put(0, 0);
        List<Edition> editions = new ArrayList<>();
        List<EnclosingStatement> newStatements = new ArrayList<>();
        int lastInsertPosition = 0;
        List<EnclosingStatement> toInsert = new ArrayList<>();
        for (EnclosingStatement original : getSortedOriginalStatements()) {
            TimeRange shouldBe = myEventGroup.getTimeline().impose(original.getTimeRange());
            EnclosingStatement is = statements.get(i);
            if (i < statements.size() && original.getId() == is.getId()) {
                if (!shouldBe.isValid()) {
                    editions.add(Edition.makeDeletion(elements.get(i).getTextRange()));
                } else if (!shouldBe.equals(is.getTimeRange())) {
                    List<TextRange> textRanges = findTextRangesOfTimeOffsets(elements.get(i));
                    editions.add(Edition.makeReplacement(textRanges.get(0),
                            Utils.formatTimeRange(shouldBe.getStartOffsetMs())));
                    if (is.isWide()) {
                        editions.add(Edition.makeReplacement(textRanges.get(1),
                                Utils.formatTimeRange(shouldBe.getEndOffsetMs())));
                    }
                    newStatements.add(new EnclosingStatement(is.getWord(), is.getTimeRange(), is.getId(), is.getDepth(), is.isWide()));
                } else {
                    newStatements.add(is);
                }
                List<DslMetaComment> comments = PsiTreeUtil.getChildrenOfTypeAsList(elements.get(i).getFirstChild(), DslMetaComment.class);
                if (is.isWide()) {
                    depthToinsertPositions.put(is.getDepth(), comments.get(1).getTextRange().getEndOffset() + 1);
                    depthToinsertPositions.put(is.getDepth() + 1, comments.get(0).getTextRange().getEndOffset() + 1);
                } else {
                    depthToinsertPositions.put(is.getDepth(), comments.get(0).getTextRange().getEndOffset() + 1);
                }
                ++i;
            } else if (shouldBe.isValid()) {
                EnclosingStatement k = new EnclosingStatement(original.getWord(),
                        shouldBe, original.getId(), original.getDepth(), original.isWide());
                newStatements.add(k);
                if (!toInsert.isEmpty() && !toInsert.get(0).getTimeRange().contains(k.getTimeRange())) {
                    editions.add(Edition.makeInsertion(lastInsertPosition, getStatement(toInsert)));
                    toInsert.clear();
                } else {
                    if (toInsert.isEmpty()) {
                        lastInsertPosition = depthToinsertPositions.get(k.getDepth());
                    }
                }
                toInsert.add(k);
            }
        }
        if (!toInsert.isEmpty()) {
            editions.add(Edition.makeInsertion(lastInsertPosition, getStatement(toInsert)));
        }
        if (!editions.isEmpty()) {
            SynchronizationManager.getInstance().addAffectedDocument(doc);
        }
        Utils.processEditions(getProject(), editions, doc);
        return newStatements;
    }

    private List<TextRange> findTextRangesOfTimeOffsets(DslStatement dslStatement) {
        return PsiTreeUtil.getChildrenOfTypeAsList(dslStatement.getFirstChild(), DslMetaComment.class).stream()
                .map(x -> PsiTreeUtil.getChildOfType(x, DslTimeOffset.class))
                .filter(Objects::nonNull)
                .map(PsiElement::getTextRange)
                .collect(Collectors.toCollection(SmartList::new));
    }

    @Override
    protected UpdateResult updateModel(List<EnclosingStatement> freshStatements) {
        Timeline timeline = myEventGroup.getTimeline();
        List<EnclosingStatement> currentStatements = myCurrentStatements.entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .sorted()
                .collect(Collectors.toList());
        Document doc = getDocument();
        if (doc == null) {
            return UpdateResult.FAILED;
        }
        int i = 0;
        for (Statement cur : freshStatements) {
            Statement prev = myCurrentStatements.get(cur.getId());
            TimeRange shouldBe = myEventGroup.getTimeline().impose(myOriginalStatements.get(cur.getId()).getTimeRange());
            if (prev == null || !shouldBe.equals(cur.getTimeRange())) {
                // For now, lets consider only deletions as valid changes.
                return UpdateResult.FAILED;
            }
        }
        boolean changed = false;
        List<DslStatement> elements = new ArrayList<>();
        visitFile((element, statement) -> elements.add(element), getPsiFile());
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
                    List<TextRange> textRanges = findTextRangesOfTimeOffsets(elements.get(i));
                    EnclosingStatement is = freshStatements.get(i);
                    editions.add(Edition.makeReplacement(textRanges.get(0),
                            Utils.formatTimeRange(shouldBe.getStartOffsetMs())));
                    if (freshStatements.get(i).isWide()) {
                        editions.add(Edition.makeReplacement(textRanges.get(1),
                                Utils.formatTimeRange(shouldBe.getEndOffsetMs())));
                    }
                    freshStatements.set(i, new EnclosingStatement(is.getWord(), shouldBe, is.getId(), is.getDepth(), is.isWide()));
                }
                ++i;
            }
        }
        if (editions.size() > 0) changed = true;
        Utils.processEditions(getProject(), editions, doc);
        return changed ? UpdateResult.UPDATED : UpdateResult.NOT_UPDATED;
    }

    /**
     * {@link EnclosingStatement} is represented by a list of it and statements it enclosed.
     * Every statement in list is contained in the first one.
     *
     * @param statements is sorted.
     * @return string representation of <tt>statements</tt>
     */
    private String getStatement(List<EnclosingStatement> statements) {
        StringBuilder builder = new StringBuilder();
        constructStatement(0, statements, builder);
        return builder.toString();
    }

    private void constructStatement(int pos, List<EnclosingStatement> statements, StringBuilder builder) {
        if (pos == statements.size()) return;
        EnclosingStatement current = statements.get(pos);
        for (int i = 0; i < current.getDepth(); ++i) {
            builder.append(UNIT_OFFSET);
        }
        builder.append(current.getWord());
        LOG.info("Text: '" + current.getWord() + "'");
        if (current.isWide()) {
            builder.append(" {");
        }
        builder.append(" // ");
        builder.append(Utils.formatTimeRange(current.getTimeRange().getStartOffsetMs()));
        builder.append(", ");
        builder.append(Utils.formatId(current.getId()));
        builder.append('\n');
        constructStatement(pos + 1, statements, builder);
        if (current.isWide()) {
            for (int i = 0; i < current.getDepth(); ++i) {
                builder.append(UNIT_OFFSET);
            }
            builder.append("} // ");
            builder.append(Utils.formatTimeRange(current.getTimeRange().getEndOffsetMs()));
            builder.append(", ");
            builder.append(Utils.formatId(current.getId()));
            builder.append('\n');
        }
    }

    /**
     * Traverses Psi tree of Dsl script file. Visits statements in the order they appear in file.
     */
    private void visitFile(BiConsumer<DslStatement, EnclosingStatement> consumer, PsiFile file) {
        PsiTreeUtil.getChildrenOfTypeAsList(file, DslStatement.class).forEach(statement -> visitStatement(consumer, statement, 1));
    }

    private void visitStatement(BiConsumer<DslStatement, EnclosingStatement> consumer, DslStatement top, int depth) {
        PsiElement start = top.getFirstChild();
        EnclosingStatement myTimeRange;
        List<DslMetaComment> comments = PsiTreeUtil.getChildrenOfTypeAsList(start, DslMetaComment.class);
        int id = Utils.parseId(Objects.requireNonNull(PsiTreeUtil.getChildOfType(comments.get(0), DslId.class)).getText());
        String word = start.getFirstChild().getText();
        if (comments.size() == 1) {
            int offset = extractOffset(comments.get(0));
            myTimeRange = new EnclosingStatement(word, new TimeRange(offset, offset), id, depth - 1, false);
        } else {
            // Otherwise, comments.size() == 2 due to assumption that file has no error elements
            int startOffset = extractOffset(comments.get(0));
            int endOffset = extractOffset(comments.get(1));
            myTimeRange = new EnclosingStatement(word, new TimeRange(startOffset, endOffset), id, depth - 1, true);
        }
        consumer.accept(top, myTimeRange);
        PsiTreeUtil.getChildrenOfTypeAsList(start, DslStatement.class).forEach(statement -> visitStatement(consumer, statement, depth + 1));
    }

    /**
     * Collects all statements in the validFile in the order they appear in it.
     *
     * @return collected statements.
     */
    @Override
    public List<EnclosingStatement> collectStatements() {
        DslOrderAdder adder = new DslOrderAdder();
        visitFile((element, statement) -> adder.add(statement), getPsiFile());
        return adder.getAdded();
    }

    @Override
    public ListenerType getType() {
        return ListenerType.SCRIPT;
    }

    @Override
    public String toString() {
        return "DslSynchronizer{" + myUrl.substring(Math.max(myUrl.length() - 25, 0)) + "}";
    }

    /**
     * Collects TimeRanges and checks if they have been added in the correct order (specified by current script grammar).
     */
    static class DslOrderAdder extends OrderAdder<EnclosingStatement> {
        private final Stack<EnclosingStatement> scopeStack = ContainerUtil.newStack();

        @Override
        public boolean checkOrder(EnclosingStatement statement) {
            if (!statement.getTimeRange().isValid()) {
                return false;
            }
            if (scopeStack.empty()) {
                scopeStack.add(statement);
                return true;
            }
            EnclosingStatement lastAdded = scopeStack.pop();
            while (!scopeStack.empty() && lastAdded.getDepth() > statement.getDepth()) {
                lastAdded = scopeStack.pop();
            }
            if (lastAdded.getId() >= statement.getId()) {
                return false;
            }
            if (statement.getDepth() == lastAdded.getDepth()) {
                if (lastAdded.getTimeRange().getEndOffsetMs() > statement.getTimeRange().getStartOffsetMs()
                        || (!scopeStack.empty() && !scopeStack.peek().getTimeRange().contains(statement.getTimeRange()))) {
                    return false;
                }
            } else {
                scopeStack.add(lastAdded);
                if (!lastAdded.getTimeRange().contains(statement.getTimeRange())) {
                    return false;
                }
            }
            scopeStack.add(statement);
            return true;
        }
    }
}
