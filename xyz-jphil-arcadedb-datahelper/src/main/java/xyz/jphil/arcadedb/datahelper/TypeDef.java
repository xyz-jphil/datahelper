package xyz.jphil.arcadedb.datahelper;

import java.util.List;
import xyz.jphil.datahelper.DataHelper_I;

/**
 * Interface representing a type definition for ArcadeDB schema initialization.
 * Defines the structure of a document type including fields and indexes.
 */
public interface TypeDef<E extends DataHelper_I<E>> {

    /**
     * Get the class definition for this type.
     *
     * @return the class that defines this document type
     */
    Class<E> definition();

    /**
     * Get the ArcadeDB type (DOCUMENT, VERTEX, or EDGE).
     *
     * @return the ArcadeType for this schema
     */
    default ArcadeType arcadeType() {
        return ArcadeType.DOCUMENT; // Default for backward compatibility
    }

    /**
     * Get the list of field names to be created in the schema.
     *
     * @return list of field names
     */
    List<String> fields();

    /**
     * Get the list of LINK fields (schema-only, not in regular fields list).
     * These will be registered with Type.LINK in the schema.
     *
     * @return list of LINK field objects
     */
    default List<xyz.jphil.datahelper.Field_I<E, ?>> linkFields() {
        return List.of(); // Default: no LINK fields
    }

    /**
     * Get full-text search indexes to create.
     *
     * @return list of field name arrays for full-text indexes
     */
    List<String[]> fullStringIndexes();

    /**
     * Get LSM tree indexes to create.
     *
     * @return list of field name arrays for LSM tree indexes
     */
    List<String[]> lsmIndexes();

    /**
     * Get unique indexes to create.
     *
     * @return list of field name arrays for unique indexes
     */
    List<String[]> uniqueIndexes();
}
