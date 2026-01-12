package xyz.jphil.datahelper;

import java.util.Map;

/**
 * JVM implementation of MapLike that wraps java.util.Map.
 * This is a simple, zero-overhead wrapper for standard Java Map.
 *
 * <p>This implementation is used by default in JVM environments and provides
 * direct pass-through to the underlying Map with no type conversions needed.</p>
 */
public class JvmMapLike implements MapLike {
    private final Map<String, Object> map;

    /**
     * Create a JvmMapLike wrapping the given Map.
     *
     * @param map the map to wrap
     */
    public JvmMapLike(Map<String, Object> map) {
        this.map = map;
    }

    @Override
    public Object get(String key) {
        return map.get(key);
    }

    @Override
    public void set(String key, Object value) {
        map.put(key, value);
    }

    @Override
    public boolean has(String key) {
        return map.containsKey(key);
    }

    @Override
    public Iterable<String> keys() {
        return map.keySet();
    }

    /**
     * Unwrap to get the underlying Map.
     * Useful for compatibility with existing code that expects Map.
     *
     * @return the underlying Map
     */
    public Map<String, Object> unwrap() {
        return map;
    }

    /**
     * Wrap an existing Map as MapLike.
     *
     * @param map the map to wrap
     * @return a MapLike wrapper around the map
     */
    public static MapLike wrap(Map<String, Object> map) {
        return new JvmMapLike(map);
    }

    // Note: getTyped() uses default implementation (no conversion needed in JVM)
}
