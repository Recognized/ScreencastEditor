package vladsaif.syncedit.plugin.synchronization;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import vladsaif.syncedit.plugin.lang.dsl.DslFileType;
import vladsaif.syncedit.plugin.lang.transcript.TranscriptFileType;

import java.nio.file.Paths;
import java.util.Set;
import java.util.function.Consumer;


public class SynchronizationDslTranscriptTest extends LightPlatformCodeInsightFixtureTestCase {

    private void doTestSynchronizer(FileType fileType, Consumer<PsiFileSynchronizer> consumer) {
        myFixture.configureByFile(getTestName(true) + "." + fileType.getDefaultExtension());
        Set<PsiFileSynchronizer> synchronizers = SynchronizationManager.getInstance().getActiveSynchronizers();
        assertEquals(1, synchronizers.size());
        PsiFileSynchronizer transcriptSync = synchronizers.iterator().next();
        consumer.accept(transcriptSync);
    }

    public void testTranscriptRangesOrderValid() {
        doTestSynchronizer(TranscriptFileType.getInstance(), this::checkOrderValid);
    }

    public void testDslRangesOrderValid() {
        doTestSynchronizer(DslFileType.getInstance(), this::checkOrderValid);
    }

    public void testTranscriptRangesOrderInvalid_01() {
        doTestSynchronizer(TranscriptFileType.getInstance(), this::checkOrderInvalid);
    }

    public void testTranscriptRangesOrderInvalid_02() {
        doTestSynchronizer(TranscriptFileType.getInstance(), this::checkOrderInvalid);
    }

    public void testDslRangesOrderInvalid_01() {
        doTestSynchronizer(DslFileType.getInstance(), this::checkOrderInvalid);
    }

    public void testDslRangesOrderInvalid_02() {
        doTestSynchronizer(DslFileType.getInstance(), this::checkOrderInvalid);
    }

    public void testDslRangesOrderInvalid_03() {
        doTestSynchronizer(DslFileType.getInstance(), this::checkOrderInvalid);
    }

    private void checkOrderValid(PsiFileSynchronizer synchronizer) {
        assertNotNull(synchronizer.collectStatements());
    }

    private void checkOrderInvalid(PsiFileSynchronizer synchronizer) {
        assertNull(synchronizer.collectStatements());
    }

    @Override
    protected String getTestDataPath() {
        return Paths.get("testdata").toAbsolutePath().toString();
    }
}
