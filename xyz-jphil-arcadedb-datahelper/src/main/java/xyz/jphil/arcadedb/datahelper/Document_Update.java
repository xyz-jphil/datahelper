package xyz.jphil.arcadedb.datahelper;

import com.arcadedb.database.MutableDocument;
import com.arcadedb.schema.Type;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Fluent API for updating ArcadeDB documents.
 * Handles field updates with optional null value handling and error handling.
 *
 * <p>Example usage:
 * <pre>
 * Document_Update.updateDocument(mutableDoc)
 *     .mapValuesWith(person.toMap())
 *     .saveDocument();
 * </pre>
 */
public class Document_Update {

    private final MutableDocument mdoc;
    private boolean updateNullValues = false;
    private boolean doNotUpdate;
    private ErrorHandler errorHandler;

    /**
     * Default error handler that logs to System.out.
     */
    public static final ErrorHandler LOG_TO_STD_OUT = new ErrorHandler() {

        @Override
        public void errorInUpdating(String fieldName, Exception ex) {
            System.out.println("Failed to update " + fieldName);
            ex.printStackTrace(System.out);
        }

        @Override
        public void skippedUpdate(String fieldName) {
            System.out.println("Skipped updating null field " + fieldName);
        }
    };

    /**
     * Interface for handling errors during document updates.
     */
    public interface ErrorHandler {

        /**
         * Called when an error occurs updating a field.
         *
         * @param fieldName the field that failed to update
         * @param ex the exception that occurred
         */
        void errorInUpdating(String fieldName, Exception ex);

        /**
         * Called when a field update is skipped (usually due to null value).
         *
         * @param fieldName the field that was skipped
         */
        void skippedUpdate(String fieldName);
    }

    /**
     * Enable updating null values (default is to skip null values).
     *
     * @return this Document_Update instance
     */
    public Document_Update updateNullValues() {
        return updateNullValues(true);
    }

    /**
     * Set error handler for update operations.
     *
     * @param errorHandler the error handler
     * @return this Document_Update instance
     */
    public Document_Update errorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    /**
     * Configure whether to update null values.
     *
     * @param updateNullValues true to update null values, false to skip them
     * @return this Document_Update instance
     */
    public Document_Update updateNullValues(boolean updateNullValues) {
        this.updateNullValues = updateNullValues;
        return this;
    }

    public Document_Update(MutableDocument mdoc) {
        this(mdoc, false);
    }

    public Document_Update(MutableDocument mdoc, boolean doNotUpdate) {
        this.mdoc = mdoc;
        this.doNotUpdate = doNotUpdate;
    }

    /**
     * Static factory method to create a Document_Update.
     *
     * @param md the mutable document to update
     * @return new Document_Update instance
     */
    public static Document_Update updateDocument(MutableDocument md) {
        return new Document_Update(md);
    }

    /**
     * Update fields from a FieldNameToValuesMapProvider.
     *
     * @param mapProvider the map provider
     * @return this Document_Update instance
     */
    public Document_Update with(FieldNameToValuesMapProvider mapProvider) {
        for (var e : mapProvider.map().entrySet()) {
            __(e.getKey(), () -> e.getValue());
        }
        return this;
    }

    /**
     * Update fields from a Map.
     *
     * @param map map of field names to values
     * @param fields optional field names to copy (if empty, copies all fields)
     * @return this Document_Update instance
     */
    public Document_Update from(Map<String, Object> map, String... fields) {
        if (fields == null || fields.length == 0) {
            // Map all fields
            for (var e : map.entrySet()) {
                __(e.getKey(), () -> e.getValue());
            }
        } else {
            // Map only specified fields
            for (String field : fields) {
                if (map.containsKey(field)) {
                    __(field, () -> map.get(field));
                }
            }
        }
        return this;
    }

    /**
     * Update fields directly from a DataHelper object.
     *
     * @param dataHelper the DataHelper object to read values from
     * @param fields optional field names to copy (if empty, copies all fields)
     * @return this Document_Update instance
     */
    public Document_Update from(xyz.jphil.datahelper.DataHelper_I<?> dataHelper, String... fields) {
        if (fields == null || fields.length == 0) {
            // Map all fields
            for (String fieldName : dataHelper.fieldNames()) {
                __(fieldName, () -> dataHelper.getPropertyByName(fieldName));
            }
        } else {
            // Map only specified fields
            for (String fieldName : fields) {
                __(fieldName, () -> dataHelper.getPropertyByName(fieldName));
            }
        }
        return this;
    }

    /**
     * Update a single field with a value from a Supplier.
     *
     * @param field the field name
     * @param valueProvider supplier of the field value
     * @return this Document_Update instance
     */
    public Document_Update __(String field, Supplier<?> valueProvider) {
        assert valueProvider != null;
        assert field != null;
        try {
            var val = valueProvider.get();
            if (val != null || (val == null && updateNullValues)) {
                if (!doNotUpdate) {
                    set(field, val);
                }
            } else {
                if (errorHandler != null) {
                    errorHandler.skippedUpdate(field);
                }
            }
        } catch (Exception e) {
            if (errorHandler != null) {
                errorHandler.errorInUpdating(field, e);
            }
        }
        return this;
    }

    /**
     * Set a field value on the document.
     *
     * @param field the field name
     * @param val the value to set
     */
    private void set(String field, Object val) {
        var mySchema = mdoc.getDatabase().getSchema().getType(mdoc.getTypeName());
        if (mySchema.getProperty(field).getType() == Type.EMBEDDED) {
            throw new UnsupportedOperationException("We are currently not supporting embedded class fields");
        } else {
            mdoc.set(field, val);
        }
    }

    /**
     * Save the document and return it.
     *
     * @return the saved MutableDocument
     */
    public MutableDocument saveDocument() {
        if (!doNotUpdate) {
            mdoc.save();
        }
        return mdoc;
    }
}
