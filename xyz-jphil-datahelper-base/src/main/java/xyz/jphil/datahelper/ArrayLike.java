package xyz.jphil.datahelper;

/**
 * Platform-agnostic abstraction over array/list structures.
 * Enables DataHelper to work with List (JVM), JSArray (TeaVM),
 * and other sequential data structures.
 *
 * <p>This interface provides a unified API for accessing indexed data
 * across different platforms.</p>
 */
public interface ArrayLike {

    /**
     * Get element at index.
     *
     * @param index the index
     * @return the element
     */
    Object get(int index);

    /**
     * Set element at index.
     *
     * @param index the index
     * @param value the value
     */
    void set(int index, Object value);

    /**
     * Get size of array.
     *
     * @return the size
     */
    int size();

    /**
     * Get typed element with type hint.
     *
     * <p>Similar to MapLike.getTyped(), allows platform-specific conversions.</p>
     *
     * <p>Default implementation just calls get(index) and ignores type hint.
     * Platform-specific implementations can override for type conversions.</p>
     *
     * @param index the index
     * @param expectedType the expected Java type
     * @return the element, potentially converted
     */
    default Object getTyped(int index, Class<?> expectedType) {
        return get(index);  // Default: no conversion
    }
}
