package xyz.jphil.arcadedb.datahelper;

import lombok.Data;
import xyz.jphil.datahelper.DataHelper;

/**
 * Test DTO for demonstrating ArcadeDB integration.
 * This will be processed by the DataHelper annotation processor.
 */
@DataHelper
@Data
public class TestPersonDTO implements TestPersonDTO_I<TestPersonDTO>, ArcadeDoc_I<TestPersonDTO> {
    String name;
    String email;
    Integer age;

    /**
     * Creates a TypeDef for schema initialization.
     * This method should be called to initialize the schema.
     */
    public static TypeDef typeDef() {
        return SchemaBuilder.defType(TestPersonDTO.class)
                .fields(TestPersonDTO_I.FIELDS)
                .unique("email")
                .__();
    }
}
