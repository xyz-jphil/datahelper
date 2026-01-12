package xyz.jphil.arcadedb.datahelper;

import com.arcadedb.database.Database;

import java.util.HashMap;

/**
 * Fluent builder for instance-level ArcadeDB document updates.
 *
 * <p>This class enables a more natural DSL where the DTO instance knows how to save itself:
 * <pre>
 * // Basic upsert
 * Document doc = person.in(db)
 *     .whereEq("email", person.email())
 *     .upsert();
 *
 * // Selective fields
 * Document doc = person.in(db)
 *     .whereEq("email", person.email())
 *     .from("name", "age")  // only update these fields
 *     .upsert();
 *
 * // Insert only
 * Document doc = person.in(db)
 *     .insert();
 * </pre>
 *
 * <p>Internally, this builder reuses the existing {@link Update} helper,
 * ensuring consistency with the current implementation.
 *
 * @param <E> the type of the ArcadeDoc_I implementation
 */
public class ArcadeDocUpdate<E extends ArcadeDoc_I<E>> {
    private final Database db;
    private final E source;
    private final String typeName;
    private final HashMap<String, Object> whereConditions = new HashMap<>();
    private String[] fieldsToUpdate;
    private boolean isUpsert = false;

    private ArcadeDocUpdate(Database db, E source, String typeName) {
        this.db = db;
        this.source = source;
        this.typeName = typeName;
    }

    /**
     * Create a new ArcadeDocUpdate instance from a database and source object.
     * Uses the source's dataClass() method to determine the type name automatically.
     *
     * @param db the ArcadeDB database
     * @param source the source DataHelper object
     * @param <E> the type of the ArcadeDoc_I implementation
     * @return a new ArcadeDocUpdate builder
     */
    public static <E extends ArcadeDoc_I<E>> ArcadeDocUpdate<E> from(Database db, E source) {
        String typeName = source.dataClass().getSimpleName();
        return new ArcadeDocUpdate<>(db, source, typeName);
    }

    /**
     * Add an equality condition to the WHERE clause.
     * Multiple calls add multiple conditions (AND logic).
     *
     * @param field the field name
     * @param value the value to match
     * @return this builder for fluent chaining
     */
    public ArcadeDocUpdate<E> whereEq(String field, Object value) {
        whereConditions.put(field, value);
        return this;
    }

    /**
     * Specify which fields to update.
     * If not called or empty array provided, all fields will be updated.
     *
     * @param fields the field names to update
     * @return this builder for fluent chaining
     */
    public ArcadeDocUpdate<E> from(String... fields) {
        this.fieldsToUpdate = fields;
        return this;
    }

    /**
     * Execute an upsert operation (create if doesn't exist, update if exists).
     * This is a terminal operation that executes the database operation.
     *
     * @return the ArcadeDB Document
     */
    public com.arcadedb.database.Document upsert() {
        return execute(true);
    }

    /**
     * Execute an insert operation (create new document only).
     * This is a terminal operation that executes the database operation.
     *
     * @return the ArcadeDB Document
     */
    public com.arcadedb.database.Document insert() {
        return execute(false);
    }

    /**
     * Internal method to execute the database operation.
     *
     * @param isUpsert true for upsert mode, false for insert only
     * @return the ArcadeDB Document
     */
    private com.arcadedb.database.Document execute(boolean isUpsert) {
        // If no where conditions and insert mode, create new document directly
        if (!isUpsert && whereConditions.isEmpty()) {
            var mDoc = db.newDocument(typeName);
            var docUpdate = Document_Update.updateDocument(mDoc);

            // Map values from source (all fields or selective)
            if (fieldsToUpdate != null && fieldsToUpdate.length > 0) {
                docUpdate.from(source, fieldsToUpdate);
            } else {
                docUpdate.from(source);
            }

            return docUpdate.saveDocument();
        }

        // Build the Update helper with type name
        var update = Update.use(db).select(typeName);

        // Add all WHERE conditions
        for (var entry : whereConditions.entrySet()) {
            update.whereEq(entry.getKey(), entry.getValue());
        }

        // Choose upsert or insert mode
        var docUpdate = isUpsert ? update.upsert() : update.updateNewOnly(null);

        // Map values from source (all fields or selective)
        if (fieldsToUpdate != null && fieldsToUpdate.length > 0) {
            docUpdate.from(source, fieldsToUpdate);
        } else {
            docUpdate.from(source);
        }

        // Execute and return the Document
        return docUpdate.saveDocument();
    }
}
