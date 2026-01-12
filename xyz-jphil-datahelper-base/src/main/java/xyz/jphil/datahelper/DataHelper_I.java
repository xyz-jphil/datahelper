package xyz.jphil.datahelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base interface for all DataHelper-generated interfaces.
 * Provides core MapLike-based serialization/deserialization with delegation pattern.
 *
 * <p>Generated interfaces implement the abstract property accessor methods,
 * while this base interface provides the delegated fromMapLike/toMapLike logic.</p>
 */
public interface DataHelper_I<E extends DataHelper_I<E>> {

    // ========== Abstract Methods (Implemented by Generated Code) ==========

    /**
     * Get all field names for this DTO.
     * Generated code returns an immutable List for safety.
     *
     * @return list of field names
     */
    List<String> fieldNames();

    /**
     * Get property value by name.
     * Generated code uses switch statement for performance.
     *
     * @param propertyName the property name
     * @return the property value, or null if not found
     */
    Object getPropertyByName(String propertyName);

    /**
     * Set property value by name.
     * Generated code uses switch statement for performance.
     *
     * @param propertyName the property name
     * @param value the value to set
     */
    void setPropertyByName(String propertyName, Object value);

    /**
     * Get property type by name.
     * Generated code uses switch statement for performance.
     *
     * @param propertyName the property name
     * @return the property type, or null if not found
     */
    Class<?> getPropertyType(String propertyName);

    /**
     * Create nested object for a property.
     * Generated code uses switch statement with direct instantiation (TeaVM-compatible).
     *
     * @param propertyName the property name
     * @return new instance of nested object, or null if property is not a nested DataHelper object
     */
    DataHelper_I<?> createNestedObject(String propertyName);

    /**
     * Create list element for a list property.
     * Generated code uses switch statement with direct instantiation (TeaVM-compatible).
     *
     * @param propertyName the property name (must be a list field)
     * @return new instance of list element, or null if property is not a list of DataHelper objects
     */
    DataHelper_I<?> createListElement(String propertyName);

    /**
     * Check if property is a list field.
     * Generated code uses simple equality check or switch.
     *
     * @param propertyName the property name
     * @return true if property is a List
     */
    boolean isListField(String propertyName);

    /**
     * Check if property is a nested DataHelper object field.
     * Generated code uses simple equality check or switch.
     *
     * @param propertyName the property name
     * @return true if property is a DataHelper_I object
     */
    boolean isNestedObjectField(String propertyName);

    // ========== Delegated Implementation: fromMapLike ==========

    /**
     * Populate this DTO from a MapLike structure.
     * This is the core method that works across all platforms (JVM, TeaVM, etc.).
     *
     * <p>Implementation uses the generated property accessor methods to populate fields,
     * enabling version independence and cross-platform compatibility.</p>
     *
     * @param mapLike the map-like structure
     * @return this instance for chaining
     */
    @SuppressWarnings("unchecked")
    default E fromMapLike(MapLike mapLike) {
        for (String fieldName : fieldNames()) {
            Class<?> fieldType = getPropertyType(fieldName);
            Object value = mapLike.getTyped(fieldName, fieldType);

            if (value == null) continue;

            if (value instanceof MapLike && isNestedObjectField(fieldName)) {
                // Nested DataHelper object
                DataHelper_I<?> nested = createNestedObject(fieldName);
                if (nested != null) {
                    nested.fromMapLike((MapLike) value);
                    setPropertyByName(fieldName, nested);
                }
            } else if (value instanceof ArrayLike && isListField(fieldName)) {
                // List of objects
                ArrayLike arrayLike = (ArrayLike) value;
                List<Object> list = new ArrayList<>();

                for (int i = 0; i < arrayLike.size(); i++) {
                    Object item = arrayLike.get(i);
                    if (item instanceof MapLike) {
                        // List element is a DataHelper object
                        DataHelper_I<?> element = createListElement(fieldName);
                        if (element != null) {
                            element.fromMapLike((MapLike) item);
                            list.add(element);
                        }
                    } else {
                        // List of primitives/simple types
                        list.add(item);
                    }
                }

                setPropertyByName(fieldName, list);
            } else {
                // Simple field - may need type conversion
                setPropertyByName(fieldName, convertType(value, fieldType));
            }
        }

        return (E) this;
    }

    // ========== Delegated Implementation: toMapLike ==========

    /**
     * Convert this DTO to a MapLike structure.
     *
     * @param deep if true, recursively serialize nested objects and lists;
     *             if false, nested objects are included as-is (shallow)
     * @return the map-like structure
     */
    default MapLike toMapLike(boolean deep) {
        MapLike mapLike = createMapLike();

        for (String fieldName : fieldNames()) {
            Object value = getPropertyByName(fieldName);

            if (value == null) continue;

            if (deep && value instanceof DataHelper_I) {
                // Deep serialize nested object
                mapLike.set(fieldName, ((DataHelper_I<?>) value).toMapLike(true));
            } else if (deep && value instanceof List && isListField(fieldName)) {
                // Deep serialize list
                List<?> list = (List<?>) value;
                ArrayLike arrayLike = createArrayLike();

                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof DataHelper_I) {
                        arrayLike.set(i, ((DataHelper_I<?>) item).toMapLike(true));
                    } else {
                        arrayLike.set(i, item);
                    }
                }

                mapLike.set(fieldName, arrayLike);
            } else {
                // Shallow mode or simple field
                mapLike.set(fieldName, value);
            }
        }

        return mapLike;
    }

    // ========== Convenience: JVM Map Support (Built-in) ==========

    /**
     * Populate this DTO from a java.util.Map.
     * This is a convenience wrapper around fromMapLike().
     *
     * @param map the map
     * @return this instance for chaining
     */
    default E fromMap(Map<String, Object> map) {
        return fromMapLike(JvmMapLike.wrap(map));
    }

    /**
     * Convert this DTO to a java.util.Map.
     * This is a convenience wrapper around toMapLike().
     *
     * @param deep if true, recursively serialize nested objects
     * @return the Map
     */
    default Map<String, Object> toMap(boolean deep) {
        MapLike mapLike = toMapLike(deep);
        if (mapLike instanceof JvmMapLike) {
            return ((JvmMapLike) mapLike).unwrap();
        }
        // Fallback: convert to Map
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : mapLike.keys()) {
            result.put(key, mapLike.get(key));
        }
        return result;
    }

    /**
     * Convert this DTO to a java.util.Map (shallow).
     * Convenience method that calls toMap(false).
     *
     * @return the Map (shallow serialization)
     */
    default Map<String, Object> toMap() {
        return toMap(false);
    }

    // ========== Helper Methods ==========

    /**
     * Convert value to target type.
     * Handles common numeric conversions (Number to Integer/Long/Double/Float).
     *
     * @param value the value to convert
     * @param targetType the target type
     * @return converted value, or original if no conversion needed
     */
    static Object convertType(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType == null) return value;
        if (targetType.isInstance(value)) return value;

        // Numeric conversions
        if (value instanceof Number) {
            Number num = (Number) value;
            if (targetType == Integer.class || targetType == int.class) {
                return num.intValue();
            } else if (targetType == Long.class || targetType == long.class) {
                return num.longValue();
            } else if (targetType == Double.class || targetType == double.class) {
                return num.doubleValue();
            } else if (targetType == Float.class || targetType == float.class) {
                return num.floatValue();
            } else if (targetType == Short.class || targetType == short.class) {
                return num.shortValue();
            } else if (targetType == Byte.class || targetType == byte.class) {
                return num.byteValue();
            }
        }

        return value;
    }

    /**
     * Create a new MapLike instance.
     * Default implementation creates JvmMapLike.
     * Platform-specific implementations can override.
     *
     * @return new MapLike instance
     */
    static MapLike createMapLike() {
        return new JvmMapLike(new LinkedHashMap<>());
    }

    /**
     * Create a new ArrayLike instance.
     * Default implementation creates JvmArrayLike.
     * Platform-specific implementations can override.
     *
     * @return new ArrayLike instance
     */
    static ArrayLike createArrayLike() {
        return new JvmArrayLike(new ArrayList<>());
    }
}
