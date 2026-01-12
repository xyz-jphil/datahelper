package xyz.jphil.arcadedb.datahelper;

import com.arcadedb.database.Database;
import com.arcadedb.schema.DocumentType;
import com.arcadedb.schema.Property;
import com.arcadedb.schema.Schema;
import com.arcadedb.schema.Type;
import java.util.List;
import static xyz.jphil.arcadedb.datahelper.FieldTypeExtractor.extractFieldType;

/**
 * Utility class for initializing ArcadeDB document schemas.
 * Handles schema creation, field creation, and index setup.
 *
 * <p>Example usage:
 * <pre>
 * InitDoc.initDocTypes(database,
 *     PersonType.typeDef(),
 *     AddressType.typeDef()
 * );
 * </pre>
 */
public class InitDoc {

    private final DocumentType documentType;
    private final Class<?> clzz;
    private final Database db;

    public InitDoc(Database db, DocumentType dt, Class<?> clzz) {
        this.db = db;
        this.documentType = dt;
        this.clzz = clzz;
    }

    public DocumentType documentType() {
        return documentType;
    }

    public Class<?> clzz() {
        return clzz;
    }

    /**
     * Initialize multiple document types at once.
     *
     * @param db the database instance
     * @param typeDefinitions varargs of TypeDef objects
     */
    public static void initDocTypes(Database db, TypeDef... typeDefinitions) {
        for (var typeDef : typeDefinitions) {
            initDocType(db, typeDef);
        }
    }

    /**
     * Initialize a single document type from a TypeDef.
     *
     * @param db the database instance
     * @param typeDef the type definition
     * @return InitDoc instance for further operations
     */
    public static InitDoc initDocType(Database db, TypeDef typeDef) {
        var d = InitDoc.initDocType(db, typeDef.definition());
        d.ensureFields(typeDef.fields());

        if (typeDef.uniqueIndexes() != null && !typeDef.uniqueIndexes().isEmpty()) {
            for (String[] uniqueIndex : typeDef.uniqueIndexes()) {
                d.ensureUniqueIndexOnProperties(uniqueIndex);
            }
        }

        if (typeDef.lsmIndexes() != null && !typeDef.lsmIndexes().isEmpty()) {
            for (var p : typeDef.lsmIndexes()) {
                d.ensureIndexOnProperties(p, Schema.INDEX_TYPE.LSM_TREE, false);
            }
        }

        if (typeDef.fullStringIndexes() != null && !typeDef.fullStringIndexes().isEmpty()) {
            for (var p : typeDef.fullStringIndexes()) {
                d.ensureIndexOnProperties(p, Schema.INDEX_TYPE.FULL_TEXT, false);
            }
        }

        return d;
    }

    /**
     * Initialize a document type from a class.
     *
     * @param db the database instance
     * @param clazz the class defining the document type
     * @return InitDoc instance for further operations
     */
    public static InitDoc initDocType(Database db, Class<?> clazz) {
        var CLASS = clazz.getSimpleName();
        DocumentType docType;
        var schema = db.getSchema();

        if (schema.existsType(CLASS)) {
            docType = schema.getType(CLASS);
        } else {
            docType = schema.createDocumentType(CLASS);
        }

        return new InitDoc(db, docType, clazz);
    }

    /**
     * Ensure all fields in the list exist in the schema.
     *
     * @param fields list of field names
     * @return this InitDoc instance
     */
    public InitDoc ensureFields(List<String> fields) {
        for (String field : fields) {
            ensureField(field);
        }
        return this;
    }

    /**
     * Create an index if it doesn't already exist.
     *
     * @param indexType the type of index to create
     * @param unique whether the index should be unique
     * @param propertyNames field names for the index
     */
    private void createIndexIfNotAlreadyThere(Schema.INDEX_TYPE indexType, boolean unique, String... propertyNames) {
        documentType.getOrCreateTypeIndex(indexType, unique, propertyNames);
    }

    /**
     * Ensure a unique index exists on the specified properties.
     *
     * @param fieldNames field names for the unique index
     * @return this InitDoc instance
     */
    public InitDoc ensureUniqueIndexOnProperties(String[] fieldNames) {
        createIndexIfNotAlreadyThere(Schema.INDEX_TYPE.LSM_TREE, true, fieldNames);
        return this;
    }

    /**
     * Ensure an index exists on the specified properties.
     *
     * @param fieldNames field names for the index
     * @param indexType type of index to create
     * @param unique whether the index should be unique
     * @return this InitDoc instance
     */
    public InitDoc ensureIndexOnProperties(String[] fieldNames, Schema.INDEX_TYPE indexType, boolean unique) {
        createIndexIfNotAlreadyThere(indexType, unique, fieldNames);
        return this;
    }

    /**
     * Ensure a field exists in the schema.
     *
     * @param fieldName the field name
     * @return the created or existing Property
     */
    public Property ensureField(String fieldName) {
        try {
            var propClass = extractFieldType(clzz, fieldName);
            if (propClass == null) {
                throw new IllegalStateException("No property named " + fieldName + " found in " + clzz);
            }

            Property p;
            if (!documentType.existsProperty(fieldName)) {
                if (documentType.getSchema().existsType(propClass.getSimpleName())) {
                    // assume non-standard embedded type
                    p = documentType.createProperty(fieldName, Type.EMBEDDED);
                } else {
                    p = documentType.createProperty(fieldName, propClass);
                }
            } else {
                p = documentType.getProperty(fieldName);
            }
            return p;
        } catch (Exception e) {
            System.err.println("Failed creating field " + fieldName + " of " + clzz);
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }
}
