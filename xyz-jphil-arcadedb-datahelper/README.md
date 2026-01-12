# xyz-jphil-arcadedb-datahelper

ArcadeDB integration for the DataHelper annotation processor framework.

## Overview

This module provides seamless integration between the DataHelper annotation processor and ArcadeDB, combining:
- Schema initialization utilities (ported from xyz-jphil-arcadedb-initialize_document_schema v1.3)
- CRUD helper utilities (ported from xyz-jphil-arcadedb-document_crud_helper v1.3)
- New DataHelper trait interface for automatic serialization/deserialization

## Maven Coordinates

```xml
<dependency>
    <groupId>io.github.xyz-jphil</groupId>
    <artifactId>xyz-jphil-arcadedb-datahelper</artifactId>
    <version>1.0</version>
</dependency>
```

## Requirements

- Java 21+
- ArcadeDB 25.7.1+
- Lombok 1.18.42
- xyz-jphil-datahelper-base 1.0
- xyz-jphil-datahelper-processor 1.0 (annotation processor)

## Quick Start

### 1. Define Your DTO

```java
@DataHelper
@Getter
@Setter
public class PersonDTO implements PersonDTO_I<PersonDTO>, DataHelper_ArcadeDB_Trait<PersonDTO> {
    String name;
    String email;
    Integer age;

    public static TypeDef typeDef() {
        return SchemaBuilder.defType(PersonDTO.class)
                .fields(PersonDTO_I.FIELDS)
                .unique("email")
                .__();
    }
}
```

### 2. Initialize Schema

```java
Database db = server.getDatabase("mydb");
InitDoc.initDocTypes(db, PersonDTO.typeDef());
```

### 3. Save Data

```java
PersonDTO person = new PersonDTO();
person.setName("John Doe");
person.setEmail("john@example.com");
person.setAge(30);

db.transaction(() -> {
    Update.use(db)
        .select("PersonDTO")
        .whereEq("email", person.getEmail())
        .upsert()
        .mapValuesWith(person)  // Direct - no intermediate Map conversion!
        .saveDocument();
});

// Save only specific fields
db.transaction(() -> {
    Update.use(db)
        .select("PersonDTO")
        .whereEq("email", "john@example.com")
        .upsert()
        .mapValuesWith(person, "name", "age")  // Only update name and age
        .saveDocument();
});
```

### 4. Load Data

```java
var docs = db.select()
    .fromType("PersonDTO")
    .where().property("email").eq().value("john@example.com")
    .documents();

if (docs.hasNext()) {
    PersonDTO person = new PersonDTO();
    person.fromArcadeDocument(docs.next());
    System.out.println(person.getName()); // "John Doe"
}
```

## Features

### Schema Initialization

- **FieldTypeExtractor**: ASM-based bytecode analysis for field type detection
- **TypeDef/SchemaBuilder**: Fluent API for defining document schemas
- **InitDoc**: Automated schema creation with support for:
  - Field definitions
  - Unique indexes
  - LSM tree indexes
  - Full-text search indexes

### CRUD Operations

- **Update**: Fluent upsert (update or insert) API
- **Document_Update**: Field-by-field update with null handling and error handling
- Supports modern lambda-based transactions
- Type-safe field access using generated interfaces

### DataHelper Integration

- **DataHelper_ArcadeDB_Trait**: Adds deserialization methods to DTOs
  - `fromArcadeDocument()`: Load from ArcadeDB Document
  - `fromArcadeMap()`: Load from plain Map
- **Document_Update**: Works directly with DataHelper objects
  - `mapValuesWith(dataHelper)`: Map all fields
  - `mapValuesWith(dataHelper, "field1", "field2")`: Map only specified fields
  - No intermediate Map conversion needed!
- Recursive handling of:
  - Nested DataHelper objects
  - Lists of DataHelper objects
  - Maps with DataHelper values
- Automatic type conversions

## Complete Example

See `src/test/java/xyz/jphil/arcadedb/datahelper/ArcadeDBIntegrationTest.java` for a complete working example demonstrating:
- Database initialization
- Schema setup
- CRUD operations
- Query patterns
- Round-trip serialization

## API Reference

### Schema Builder

```java
TypeDef typeDef = SchemaBuilder.defType(MyClass.class)
    .fields(MyClass_I.FIELDS)
    .unique("id")
    .lsmIndex("lastName")
    .fullStringIndex("description")
    .__();
```

### Update Helper

```java
Update.use(db)
    .select("TypeName")
    .whereEq("fieldName", value)
    .upsert(exists -> System.out.println("Exists: " + exists))
    .__("field1", () -> value1)
    .__("field2", () -> value2)
    .mapValuesWith(map)
    .saveDocument();
```

### Trait Methods

```java
// Load from Document
dto.fromArcadeDocument(arcadeDoc);

// Load from Map
dto.fromArcadeMap(map);

// Save directly (no toMap() needed!)
Update.use(db)
    .select("TypeName")
    .whereEq("id", dto.getId())
    .upsert()
    .mapValuesWith(dto)  // All fields
    // OR
    .mapValuesWith(dto, "field1", "field2")  // Selected fields only
    .saveDocument();
```

## Architecture

This module follows the patterns documented in:
- `arcade-db-working-examples.md` - Concrete implementation patterns
- `arcade-db-usage-guide.md` - Architectural guidance

The integration leverages DataHelper's property accessor interface (`DataHelper_I`) to provide generic serialization without code generation or reflection.

## License

Same as parent project.

## Related Modules

- `xyz-jphil-datahelper-base` - Core DataHelper interfaces
- `xyz-jphil-datahelper-processor` - Annotation processor
- `xyz-jphil-datahelper-annotations` - @DataHelper annotation
