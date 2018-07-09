package vladsaif.syncedit.plugin.speechrecognition;

import vladsaif.syncedit.plugin.editor.ClosedLongRange;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Transcript {
    private final Map<TimeRange, String> myWords = new TreeMap<>();

    public Transcript(Map<TimeRange, String> words) {
        myWords.putAll(words);
    }

    public String getText() {
        return myWords.entrySet().stream().map(Map.Entry::getValue).map(Object::toString).collect(Collectors.joining(" "));
    }

    public Map<TimeRange, String> getWords() {
        return Collections.unmodifiableMap(myWords);
    }
}
