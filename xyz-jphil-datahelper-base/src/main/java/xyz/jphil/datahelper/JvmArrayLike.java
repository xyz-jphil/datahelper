package xyz.jphil.datahelper;

import java.util.List;

/**
 * JVM implementation of ArrayLike that wraps java.util.List.
 * This is a simple, zero-overhead wrapper for standard Java List.
 *
 * <p>This implementation is used by default in JVM environments and provides
 * direct pass-through to the underlying List.</p>
 */
public class JvmArrayLike implements ArrayLike {
    private final List<Object> list;

    /**
     * Create a JvmArrayLike wrapping the given List.
     *
     * @param list the list to wrap
     */
    public JvmArrayLike(List<Object> list) {
        this.list = list;
    }

    @Override
    public Object get(int index) {
        return list.get(index);
    }

    @Override
    public void set(int index, Object value) {
        // Expand list if needed
        while (index >= list.size()) {
            list.add(null);
        }
        list.set(index, value);
    }

    @Override
    public int size() {
        return list.size();
    }

    /**
     * Unwrap to get the underlying List.
     * Useful for compatibility with existing code that expects List.
     *
     * @return the underlying List
     */
    public List<Object> unwrap() {
        return list;
    }

    /**
     * Wrap an existing List as ArrayLike.
     *
     * @param list the list to wrap
     * @return an ArrayLike wrapper around the list
     */
    public static ArrayLike wrap(List<Object> list) {
        return new JvmArrayLike(list);
    }

    // Note: getTyped() uses default implementation
}
