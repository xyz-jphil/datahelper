package xyz.jphil.arcadedb.datahelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent builder for creating TypeDef objects.
 * Provides a convenient API for defining document types with fields and indexes.
 *
 * <p>Example usage:
 * <pre>
 * TypeDef personType = defType(Person.class)
 *     .fields(PersonSymbols.FIELDS)
 *     .unique(PersonSymbols.$email)
 *     .lsmIndex(PersonSymbols.$lastName)
 *     .__();
 * </pre>
 */
public class SchemaBuilder implements TypeDef {

    private final Class<?> definition;
    private List<String> fields = new ArrayList<>();
    private List<String[]> uniqueIndexes = new ArrayList<>();
    private List<String[]> lsmIndexes = new ArrayList<>();
    private List<String[]> fullStringIndexes = new ArrayList<>();

    public SchemaBuilder(Class<?> definition) {
        this.definition = definition;
    }

    /**
     * Create LSM tree indexes for multiple single fields.
     *
     * @param ls field names
     * @return this builder
     */
    public SchemaBuilder lsmIndexForEach(String... ls) {
        for (String l : ls) {
            this.lsmIndexes.add(new String[] { l });
        }
        return this;
    }

    /**
     * Create LSM tree index (can be composite).
     *
     * @param l field name(s)
     * @return this builder
     */
    public SchemaBuilder lsmIndex(String... l) {
        this.lsmIndexes.add(l);
        return this;
    }

    /**
     * Create unique index (can be composite).
     *
     * @param l field name(s)
     * @return this builder
     */
    public SchemaBuilder unique(String... l) {
        this.uniqueIndexes.add(l);
        return this;
    }

    /**
     * Create full-text search index (can be composite).
     *
     * @param l field name(s)
     * @return this builder
     */
    public SchemaBuilder fullStringIndex(String... l) {
        this.fullStringIndexes.add(l);
        return this;
    }

    /**
     * Add multiple fields at once.
     *
     * @param fields list of field names
     * @return this builder
     */
    public SchemaBuilder fields(List<String> fields) {
        this.fields.addAll(fields);
        return this;
    }

    /**
     * Add a single field.
     *
     * @param field field name
     * @return this builder
     */
    public SchemaBuilder field(String field) {
        this.fields.add(field);
        return this;
    }

    /**
     * Static factory method to create a new SchemaBuilder.
     *
     * @param definition the class defining the document type
     * @return new SchemaBuilder instance
     */
    public static SchemaBuilder defType(Class<?> definition) {
        return new SchemaBuilder(definition);
    }

    @Override
    public Class<?> definition() {
        return definition;
    }

    @Override
    public List<String> fields() {
        return fields;
    }

    @Override
    public List<String[]> uniqueIndexes() {
        return uniqueIndexes;
    }

    @Override
    public List<String[]> lsmIndexes() {
        return lsmIndexes;
    }

    @Override
    public List<String[]> fullStringIndexes() {
        return fullStringIndexes;
    }

    /**
     * Finalize the builder and return as TypeDef.
     * The __ method provides a nice visual terminator in fluent chains.
     *
     * @return this as TypeDef
     */
    public TypeDef __() {
        return this;
    }
}
