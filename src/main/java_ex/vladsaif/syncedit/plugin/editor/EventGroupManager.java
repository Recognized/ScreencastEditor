package vladsaif.syncedit.plugin.editor;

import com.google.common.base.Charsets;
import vladsaif.syncedit.plugin.editor.EditionListener.ListenerType;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Not thread-safe, all methods are designed to be invoked in EDT.
 */
public class EventGroupManager {
    private static final EventGroupManager INSTANCE = new EventGroupManager();
    private final HashMap<Path, EventGroupImpl> groups = new HashMap<>();

    public static EventGroupManager getInstance() {
        return INSTANCE;
    }

    /**
     * Get group of specified <tt>file</tt>.
     *
     * @param file in group.
     */
    public EventGroup getGroup(Path file) {
        return new EventGroupProxy(file);
    }

    public static String getFileNameWithoutExtension(Path file) {
        if (file.getNameCount() == 0) return file.toAbsolutePath().toString();
        String name = file.getName(file.getNameCount() - 1).toString();
        if (name.lastIndexOf('.') < 0) return name;
        return file.toAbsolutePath()
                .resolveSibling(name.substring(0, name.lastIndexOf('.')))
                .toString();
    }

    public void loadMapping(List<Path> paths) throws InvalidMapping {
        if (paths.isEmpty()) return;
        List<EventGroupImpl> toMerge = groups.entrySet()
                .stream()
                .filter(x -> paths.contains(x.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        List<Path> toUpdate = groups.entrySet()
                .stream()
                .filter(x -> toMerge.contains(x.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        if (toMerge.isEmpty()) {
            EventGroupImpl eventGroup = new EventGroupImpl(paths.get(0));
            paths.forEach(x -> groups.put(x, eventGroup));
            return;
        }
        EventGroupImpl res = toMerge.get(0);
        for (int i = 1; i < toMerge.size(); ++i) {
            res = merge(res, toMerge.get(i));
        }
        EventGroupImpl effectivelyFinal = res;
        toUpdate.forEach(x -> groups.put(x, effectivelyFinal));
        paths.forEach(x -> groups.put(x, effectivelyFinal));
    }

    public static Map<Path, ListenerType> parseMappingFile(Path mappingFile) throws ParseException, IOException {
        Map<Path, ListenerType> ans = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(mappingFile), Charsets.UTF_8))) {
            String line;
            int offset = 0;
            Set<String> validKeys = Arrays.stream(ListenerType.values())
                    .map(Object::toString)
                    .collect(Collectors.toSet());
            Set<String> availableKeys = new HashSet<>();
            availableKeys.add(ListenerType.TRANSCRIPT.name());
            availableKeys.add(ListenerType.SCRIPT.name());
            List<String> alwaysAvailableKeys = Arrays.asList(ListenerType.AUDIO.name(), ListenerType.VIDEO.name());
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                int sep = line.indexOf('=');
                if (sep == -1) {
                    throw new ParseException("Unexpected input: " + line.substring(0, Math.min(line.length(), 80))
                            + ". Grammar: (<type key>=<path>\\n)*", offset);
                }
                String key = line.substring(0, sep).toUpperCase(Locale.ENGLISH).trim();
                if (validKeys.contains(key)) {
                    if (!alwaysAvailableKeys.contains(key) && !availableKeys.remove(key)) {
                        throw new ParseException("Duplicate declaration of " + key + " key", offset);
                    }
                } else {
                    throw new ParseException("Invalid key: " + key + ". Valid keys: "
                            + validKeys.stream()
                            .map(x -> "\"" + x + "\"")
                            .collect(Collectors.joining(","))
                            + ".", offset);
                }
                String value = line.substring(sep + 1).trim();
                if (value.isEmpty()) {
                    throw new ParseException("Empty value: " + line.substring(0, Math.min(line.length(), 80)), offset);
                }
                File file = new File(value);
                Path res;
                try {
                    if (file.isAbsolute()) {
                        res = file.toPath();
                    } else {
                        res = mappingFile.toAbsolutePath().resolveSibling(value);
                    }
                } catch (InvalidPathException | SecurityException ex) {
                    throw new ParseException(ex.getMessage(), offset);
                }
                ans.put(res, ListenerType.valueOf(key.toUpperCase(Locale.ENGLISH)));
                offset += line.length();
            }
        }
        return ans;
    }

    protected EventGroupImpl merge(EventGroupImpl first, EventGroupImpl second) throws InvalidMapping {
        if (first.dslListener != null && second.dslListener != null ||
                first.transcriptListener != null && second.transcriptListener != null) {
            throw new InvalidMapping("Mappings are intersecting.");
        }
        EventGroupImpl res = new EventGroupImpl(first);
        res.passiveListeners.addAll(second.passiveListeners);
        if (second.dslListener != null) {
            res.dslListener = second.dslListener;
        }
        if (second.transcriptListener != null) {
            res.transcriptListener = second.transcriptListener;
        }
        second.model.getDeletedRanges().forEach(res.model::delete);
        return res;
    }

    private static class EventGroupProxy implements EventGroup {
        Path myPath;

        EventGroupProxy(Path path) {
            myPath = path;
        }

        private EventGroup getRealGroup() {
            return EventGroupManager.getInstance().groups.computeIfAbsent(myPath, EventGroupImpl::new);
        }

        @Override
        public EditionListener getListener(ListenerType type) {
            return getRealGroup().getListener(type);
        }

        @Override
        public void addListener(EditionListener listener) {
            getRealGroup().addListener(listener);
        }

        @Override
        public void removeListener(EditionListener listener) {
            getRealGroup().removeListener(listener);
        }

        @Override
        public Timeline getTimeline() {
            return getRealGroup().getTimeline();
        }

        @Override
        public void notifyFiles() {
            getRealGroup().notifyFiles();
        }

        @Override
        public void notifyDsl() {
            getRealGroup().notifyDsl();
        }

        @Override
        public void notifyTranscript() {
            getRealGroup().notifyTranscript();
        }

        @Override
        public String toString() {
            return getRealGroup().toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EventGroupProxy that = (EventGroupProxy) o;
            return Objects.equals(getRealGroup(), that.getRealGroup());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getRealGroup());
        }
    }

    /**
     * Unites listeners into group so they can notify each other.
     */
    private static class EventGroupImpl implements EventGroup {
        private EditionListener dslListener;
        private EditionListener transcriptListener;
        private Set<EditionListener> passiveListeners = new HashSet<>();
        private final String myGroupKey;
        private final Timeline model = new Timeline();

        private EventGroupImpl(EventGroupImpl source) {
            myGroupKey = source.myGroupKey;
            model.load(source.model);
            passiveListeners.addAll(source.passiveListeners);
            dslListener = source.dslListener;
            transcriptListener = source.transcriptListener;
        }

        private EventGroupImpl(Path firstFile) {
            myGroupKey = getFileNameWithoutExtension(firstFile);
        }

        @Override
        public EditionListener getListener(ListenerType type) {
            switch (type) {
                case TRANSCRIPT:
                    return transcriptListener;
                case SCRIPT:
                    return dslListener;
                default:
                    return passiveListeners.stream()
                            .filter(x -> x.getType().equals(type))
                            .findAny()
                            .orElse(null);
            }
        }

        @Override
        public void addListener(EditionListener listener) {
            switch (listener.getType()) {
                case TRANSCRIPT:
                    transcriptListener = listener;
                    break;
                case SCRIPT:
                    dslListener = listener;
                    break;
                default:
                    passiveListeners.add(listener);
            }
        }

        @Override
        public void removeListener(EditionListener listener) {
            switch (listener.getType()) {
                case TRANSCRIPT:
                    transcriptListener = null;
                    break;
                case SCRIPT:
                    dslListener = null;
                    break;
                default:
                    passiveListeners.remove(listener);
            }
        }

        @Override
        public Timeline getTimeline() {
            return model;
        }

        @Override
        public void notifyFiles() {
            passiveListeners.forEach(EditionListener::onSomethingChanged);
        }

        @Override
        public void notifyDsl() {
            if (dslListener != null) {
                dslListener.onSomethingChanged();
            }
        }

        @Override
        public void notifyTranscript() {
            if (transcriptListener != null) {
                transcriptListener.onSomethingChanged();
            }
        }

        @Override
        public String toString() {
            return "EventGroup{'" + myGroupKey + "\'}";
        }
    }


}
