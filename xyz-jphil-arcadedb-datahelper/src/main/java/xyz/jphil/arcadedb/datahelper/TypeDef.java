package xyz.jphil.arcadedb.datahelper;

import java.util.List;

/**
 * Interface representing a type definition for ArcadeDB schema initialization.
 * Defines the structure of a document type including fields and indexes.
 */
public interface TypeDef {

    /**
     * Get the class definition for this type.
     *
     * @return the class that defines this document type
     */
    Class<?> definition();

    /**
     * Get the list of field names to be created in the schema.
     *
     * @return list of field names
     */
    List<String> fields();

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
