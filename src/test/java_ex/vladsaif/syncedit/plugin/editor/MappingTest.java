package vladsaif.syncedit.plugin.editor;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Map;
import java.util.stream.Collectors;


public class MappingTest {
    private final Path resources = Paths.get("src", "test", "resources");

    private void print(Map<Path, EditionListener.ListenerType> map) {
        System.out.println(map.entrySet().stream().map(x -> x.getKey() + "=" + x.getValue()).collect(Collectors.joining("\n")));
    }

    private void parse(String file) throws IOException, ParseException {
        print(EventGroupManager.parseMappingFile(resources.resolve(file)));
    }

    private void testError(String file) throws IOException {
        try {
            parse(file);
        } catch (ParseException ex) {
            System.out.println(ex.getMessage());
            return;
        }
        Assert.fail("Exception did not raised");
    }

    @Test
    public void parsingMappingTest() throws Exception {
        parse("mapping_01.mapping");
    }

    @Test
    public void duplicate_transcript_key() throws Exception {
        testError("duplicate_key_01.mapping");
    }

    @Test
    public void duplicate_script_key() throws Exception {
        testError("duplicate_key_02.mapping");
    }

    @Test
    public void empty_key() throws Exception {
        testError("empty_key.mapping");
    }

    @Test
    public void empty_value() throws Exception {
        testError("empty_value.mapping");
    }

    @Test
    public void wrong_key_audios() throws Exception {
        testError("wrong_key.mapping");
    }

    @Test
    public void no_map() throws Exception {
        testError("no_map.mapping");
    }

    @Test
    public void duplicate_video_audio_key() throws Exception {
        parse("duplicate_key_03.mapping");
    }

    @Test
    public void empty_file() throws Exception {
        parse("empty_file.mapping");
    }
}
