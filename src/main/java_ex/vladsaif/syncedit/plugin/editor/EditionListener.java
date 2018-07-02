package vladsaif.syncedit.plugin.editor;

/**
 * Listener that gets notified by its {@link EventGroup}
 * when time ranges in transcript file from group has been changed.
 * Needs to be added to a corresponding group to get notified.
 *
 * @see EventGroupManager#getGroup(java.nio.file.Path)
 * @see EventGroup#addListener(EditionListener)
 */
public interface EditionListener {

    /**
     * Called when {@link Timeline} has been changed.
     *
     * @see EventGroup#getTimeline().
     */
    default void onSomethingChanged() {
    }

    ListenerType getType();

    enum ListenerType {
        AUDIO, TRANSCRIPT, SCRIPT, VIDEO
    }
}
