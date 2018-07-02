package vladsaif.syncedit.plugin.synchronization;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class which purpose is to know if all added were added in correct order
 */
abstract class OrderAdder<T extends Statement> {
    private boolean orderValid = true;
    private final List<T> added = new ArrayList<>();

    /**
     * Checks order and perform addition if order maintains.
     *
     * @param statement to add.
     * @return <tt>true</tt> if order maintains after addition, <tt>false</tt>, otherwise.
     */
    boolean add(T statement) {
        if (!orderValid) {
            return false;
        }
        if (checkOrder(statement)) {
            added.add(statement);
        } else {
            orderValid = false;
        }
        return orderValid;
    }

    /**
     * Checks whether addition of <tt>range</tt> breaks the order or not.
     *
     * @param statement that is going to be added.
     * @return <tt>true</tt> if addition does not invalidate order, <tt>false</tt>, otherwise
     */
    abstract boolean checkOrder(T statement);

    /**
     * Returns added added.
     *
     * @return objects that were added with {@link #add(Statement)} and did not break order.
     */
    List<T> getAdded() {
        return added;
    }
}
