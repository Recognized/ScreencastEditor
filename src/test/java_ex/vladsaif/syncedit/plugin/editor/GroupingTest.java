package vladsaif.syncedit.plugin.editor;

import org.junit.Test;

import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

public class GroupingTest {
    private final EventGroupManager egm = EventGroupManager.getInstance();

    @Test
    public void equality() {
        assertEquals(egm.getGroup(Paths.get("src")), egm.getGroup(Paths.get("src")));
        assertEquals(egm.getGroup(Paths.get("other")), egm.getGroup(Paths.get("other")));
        assertEquals(egm.getGroup(Paths.get("src")), egm.getGroup(Paths.get("src")));
    }

    @Test
    public void merging_01() throws InvalidMapping {
        egm.loadMapping(Stream.of("one", "two", "three").map(Paths::get).collect(Collectors.toList()));
        assertEquals(1, Stream.of("one", "two", "three").map(Paths::get).map(egm::getGroup).collect(Collectors.toSet()).size());
    }

    @Test
    public void merging_02() throws InvalidMapping {
        egm.loadMapping(Stream.of("one", "two", "three").map(Paths::get).collect(Collectors.toList()));
        egm.loadMapping(Stream.of("four", "two", "three").map(Paths::get).collect(Collectors.toList()));
        assertEquals(1, Stream.of("one", "two", "three", "four").map(Paths::get).map(egm::getGroup).collect(Collectors.toSet()).size());
    }
}
