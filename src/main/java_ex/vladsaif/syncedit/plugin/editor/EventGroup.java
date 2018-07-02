package vladsaif.syncedit.plugin.editor;

public interface EventGroup {

    EditionListener getListener(EditionListener.ListenerType type);

    void addListener(EditionListener listener);

    void removeListener(EditionListener listener);

    Timeline getTimeline();

    /**
     * Notifies all file listeners in this group about changes.
     */
    void notifyFiles();

    /**
     * Notifies all DSL listeners in this group about changes.
     */
    void notifyDsl();

    /**
     * Notifies all transcript listeners in this group about changes.
     */
    void notifyTranscript();
}
