package xyz.jphil.arcadedb.datahelper;

import com.arcadedb.database.Database;
import com.arcadedb.database.Document;
import xyz.jphil.datahelper.DataHelper_I;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Trait interface providing ArcadeDB serialization and deserialization capabilities
 * for DataHelper_I implementations.
 *
 * <p>This trait provides two ways to save documents:</p>
 *
 * <h3>1. Traditional Pattern (Still Supported)</h3>
 * <pre>
 * Update.use(db)
 *     .select("PersonDTO")
 *     .whereEq("email", person.getEmail())
 *     .upsert()
 *     .mapValuesWith(person)
 *     .saveDocument();
 * </pre>
 *
 * <h3>2. Instance-Level DSL (New, More Natural)</h3>
 * <pre>
 * // Full DSL - upsert/insert are terminal operations
 * Document doc = person.in(db)
 *     .whereEq("email", person.email())
 *     .from("name", "age")  // selective fields, omit for all
 *     .upsert();            // terminal operation, returns Document
 *
 * // Insert only
 * Document doc = person.in(db)
 *     .insert();
 * </pre>
 *
 * <p>Example usage:
 * <pre>
 * &#64;DataHelper
 * &#64;Getter
 * &#64;Setter
 * public class PersonDTO implements PersonDTO_I&lt;PersonDTO&gt;, ArcadeDoc_I&lt;PersonDTO&gt; {
 *     String name;
 *     String email;
 * }
 *
 * // Load from Document
 * PersonDTO loaded = new PersonDTO();
 * loaded.fromArcadeDocument(doc);
 * </pre>
 */
public interface ArcadeDoc_I<E extends ArcadeDoc_I<E>>
        extends DataHelper_I<E> {

    // ========== Instance-Level DSL Methods ==========

    /**
     * Start a fluent update/insert operation for this document instance.
     * This is the main entry point for the instance-level DSL.
     *
     * <p>Usage:
     * <pre>
     * Document doc = person.in(db)
     *     .whereEq("email", person.email())
     *     .from("name", "age")  // optional, omit for all fields
     *     .upsert();             // terminal operation
     * </pre>
     *
     * @param db the ArcadeDB database
     * @return a fluent builder for document operations
     */
    @SuppressWarnings("unchecked")
    default ArcadeDocUpdate<E> in(Database db) {
        return ArcadeDocUpdate.from(db, (E) this);
    }

    // ========== Deserialization Methods ==========

    /**
     * Populate this object from an ArcadeDB Document.
     * Handles nested objects, lists, and maps recursively.
     *
     * @param doc the ArcadeDB document to read from
     * @return this object for fluent chaining
     */
    @SuppressWarnings("unchecked")
    default E fromArcadeDocument(Document doc) {
        if (doc == null) {
            return (E) this;
        }

        for (String fieldName : fieldNames()) {
            if (!doc.has(fieldName)) {
                continue;
            }

            Object value = doc.get(fieldName);
            if (value == null) {
                continue;
            }

            Class<?> fieldType = getPropertyType(fieldName);
            if (fieldType == null) {
                continue;
            }

            if (isNestedObjectField(fieldName)) {
                // Nested DataHelper object
                if (value instanceof Document) {
                    // ArcadeDB Document (ImmutableEmbeddedDocument)
                    DataHelper_I<?> nested = createNestedObject(fieldName);
                    if (nested instanceof ArcadeDoc_I) {
                        ((ArcadeDoc_I<?>) nested).fromArcadeDocument((Document) value);
                        setPropertyByName(fieldName, nested);
                    }
                } else if (value instanceof Map) {
                    // Plain Map
                    DataHelper_I<?> nested = createNestedObject(fieldName);
                    if (nested instanceof ArcadeDoc_I) {
                        ((ArcadeDoc_I<?>) nested).fromArcadeMap((Map<String, Object>) value);
                        setPropertyByName(fieldName, nested);
                    }
                }
            } else if (isListField(fieldName) && value instanceof List) {
                // List field
                List<?> sourceList = (List<?>) value;
                List<Object> targetList = new ArrayList<>();

                for (Object item : sourceList) {
                    if (item instanceof Document) {
                        // ArcadeDB Document (ImmutableEmbeddedDocument)
                        DataHelper_I<?> listElement = createListElement(fieldName);
                        if (listElement instanceof ArcadeDoc_I) {
                            ((ArcadeDoc_I<?>) listElement).fromArcadeDocument((Document) item);
                            targetList.add(listElement);
                        } else {
                            targetList.add(item);
                        }
                    } else if (item instanceof Map) {
                        // Plain Map
                        DataHelper_I<?> listElement = createListElement(fieldName);
                        if (listElement instanceof ArcadeDoc_I) {
                            ((ArcadeDoc_I<?>) listElement).fromArcadeMap((Map<String, Object>) item);
                            targetList.add(listElement);
                        } else {
                            targetList.add(item);
                        }
                    } else {
                        targetList.add(item);
                    }
                }
                setPropertyByName(fieldName, targetList);
            } else if (isMapField(fieldName) && value instanceof Map) {
                // Map field
                Map<?, ?> sourceMap = (Map<?, ?>) value;
                Map<Object, Object> targetMap = (Map<Object, Object>) createMapInstance(fieldName);

                Class<?> keyType = getMapKeyType(fieldName);
                Class<?> valueType = getMapValueType(fieldName);

                for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
                    Object key = DataHelper_I.convertType(entry.getKey(), keyType);
                    Object mapValue = entry.getValue();

                    if (isMapValueDataHelper(fieldName) && mapValue instanceof Map) {
                        DataHelper_I<?> mapValueElement = createMapValueElement(fieldName);
                        if (mapValueElement instanceof ArcadeDoc_I) {
                            ((ArcadeDoc_I<?>) mapValueElement).fromArcadeMap((Map<String, Object>) mapValue);
                            targetMap.put(key, mapValueElement);
                        } else {
                            targetMap.put(key, mapValue);
                        }
                    } else {
                        Object convertedValue = DataHelper_I.convertType(mapValue, valueType);
                        targetMap.put(key, convertedValue);
                    }
                }
                setPropertyByName(fieldName, targetMap);
            } else {
                // Simple field - convert and set
                Object convertedValue = DataHelper_I.convertType(value, fieldType);
                setPropertyByName(fieldName, convertedValue);
            }
        }

        return (E) this;
    }

    /**
     * Populate this object from a Map.
     * Similar to fromArcadeDocument but works with plain Maps.
     *
     * @param map the map to read from
     * @return this object for fluent chaining
     */
    @SuppressWarnings("unchecked")
    default E fromArcadeMap(Map<String, Object> map) {
        if (map == null) {
            return (E) this;
        }

        for (String fieldName : fieldNames()) {
            if (!map.containsKey(fieldName)) {
                continue;
            }

            Object value = map.get(fieldName);
            if (value == null) {
                continue;
            }

            Class<?> fieldType = getPropertyType(fieldName);
            if (fieldType == null) {
                continue;
            }

            if (isNestedObjectField(fieldName)) {
                // Nested DataHelper object
                if (value instanceof Map) {
                    DataHelper_I<?> nested = createNestedObject(fieldName);
                    if (nested instanceof ArcadeDoc_I) {
                        ((ArcadeDoc_I<?>) nested).fromArcadeMap((Map<String, Object>) value);
                        setPropertyByName(fieldName, nested);
                    }
                }
            } else if (isListField(fieldName) && value instanceof List) {
                // List field
                List<?> sourceList = (List<?>) value;
                List<Object> targetList = new ArrayList<>();

                for (Object item : sourceList) {
                    if (item instanceof Map) {
                        DataHelper_I<?> listElement = createListElement(fieldName);
                        if (listElement instanceof ArcadeDoc_I) {
                            ((ArcadeDoc_I<?>) listElement).fromArcadeMap((Map<String, Object>) item);
                            targetList.add(listElement);
                        } else {
                            targetList.add(item);
                        }
                    } else {
                        targetList.add(item);
                    }
                }
                setPropertyByName(fieldName, targetList);
            } else if (isMapField(fieldName) && value instanceof Map) {
                // Map field
                Map<?, ?> sourceMap = (Map<?, ?>) value;
                Map<Object, Object> targetMap = (Map<Object, Object>) createMapInstance(fieldName);

                Class<?> keyType = getMapKeyType(fieldName);
                Class<?> valueType = getMapValueType(fieldName);

                for (Map.Entry<?, ?> entry : sourceMap.entrySet()) {
                    Object key = DataHelper_I.convertType(entry.getKey(), keyType);
                    Object mapValue = entry.getValue();

                    if (isMapValueDataHelper(fieldName) && mapValue instanceof Map) {
                        DataHelper_I<?> mapValueElement = createMapValueElement(fieldName);
                        if (mapValueElement instanceof ArcadeDoc_I) {
                            ((ArcadeDoc_I<?>) mapValueElement).fromArcadeMap((Map<String, Object>) mapValue);
                            targetMap.put(key, mapValueElement);
                        } else {
                            targetMap.put(key, mapValue);
                        }
                    } else {
                        Object convertedValue = DataHelper_I.convertType(mapValue, valueType);
                        targetMap.put(key, convertedValue);
                    }
                }
                setPropertyByName(fieldName, targetMap);
            } else {
                // Simple field - convert and set
                Object convertedValue = DataHelper_I.convertType(value, fieldType);
                setPropertyByName(fieldName, convertedValue);
            }
        }

        return (E) this;
    }
}
