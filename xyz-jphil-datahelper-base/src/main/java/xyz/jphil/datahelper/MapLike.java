package xyz.jphil.datahelper;

/**
 * Platform-agnostic abstraction over map-like structures.
 * Enables DataHelper to work with Map (JVM), JSMapLike (TeaVM),
 * Document (ArcadeDB), and other key-value stores.
 *
 * <p>This interface provides a unified API for accessing key-value data
 * across different platforms without requiring reflection or runtime type checking.</p>
 */
public interface MapLike {

    /**
     * Get value by key.
     *
     * @param key the key
     * @return the value, or null if not present
     */
    Object get(String key);

    /**
     * Set value by key.
     *
     * @param key the key
     * @param value the value
     */
    void set(String key, Object value);

    /**
     * Check if key exists.
     *
     * @param key the key
     * @return true if key exists (even if value is null)
     */
    boolean has(String key);

    /**
     * Get all keys.
     *
     * @return iterable of all keys
     */
    Iterable<String> keys();

    /**
     * Get typed value with type hint.
     *
     * <p>This method allows platform-specific implementations to perform
     * type conversions based on the expected type. This is especially
     * useful for TeaVM where JSNumber needs conversion to Integer/Double.</p>
     *
     * <p>Default implementation just calls get(key) and ignores type hint.
     * Platform-specific implementations (like TeaVMMapLike) can override
     * this to perform proper type conversions.</p>
     *
     * @param key the key
     * @param expectedType the expected Java type (e.g., Integer.class, String.class)
     * @return the value, potentially converted to expected type
     */
    default Object getTyped(String key, Class<?> expectedType) {
        return get(key);  // Default: no conversion
    }
}
