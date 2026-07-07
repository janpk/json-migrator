# json-migrator - migration of JSON structures

| **Stack** | ![](https://github.com/janpk/json-migrator/raw/metrics/badges/java_version.svg) ![](https://github.com/janpk/json-migrator/raw/metrics/badges/kotlin_version.svg) ![](https://github.com/janpk/json-migrator/raw/metrics/badges/kotlin_language_version.svg) ![](https://github.com/janpk/json-migrator/raw/metrics/badges/jackson_version.svg) |
| --- | --- |
| **Tests** | ![](https://github.com/janpk/json-migrator/raw/metrics/badges/test_results.svg) ![](https://github.com/janpk/json-migrator/raw/metrics/badges/test_classes.svg) ![](https://github.com/janpk/json-migrator/raw/metrics/badges/test_cases.svg) ![](https://github.com/janpk/json-migrator/raw/metrics/badges/test_skipped.svg) ![](https://github.com/janpk/json-migrator/raw/metrics/badges/test_ratio.svg) |
| **Coverage** | ![](https://github.com/janpk/json-migrator/raw/metrics/badges/lines.svg) ![](https://github.com/janpk/json-migrator/raw/metrics/badges/branches.svg) ![](https://github.com/janpk/json-migrator/raw/metrics/badges/classes.svg) ![](https://github.com/janpk/json-migrator/raw/metrics/badges/instructions.svg) ![](https://github.com/janpk/json-migrator/raw/metrics/badges/methods.svg) |

[Full metrics dashboard](https://github.com/janpk/json-migrator/blob/metrics/METRICS.md)

`json-migrator` is a Kotlin DSL for transforming JSON documents between schema versions.
It applies explicit migrations to a Jackson `ObjectNode`, updates the configured version field,
and fails fast when a migration cannot be applied safely.

A typical use case is when json documents are stored in a database or received as a Kafka event. The application has a DTO structure matching the json and the DTO structure evolves over time. To avoid mass updates or mapper classes, the json migrator can be used to migrate a received json 
document just in time so that it can be deserialized into the target DTO structure. When processing is done, the DTO can be serialized back to the database reflecting the latest DTO structure. This assumes that the application can live with the situation were the database contains multiple 
versions of the json document.

## Quick start

```kotlin
val mapper = jacksonObjectMapper()
val document = mapper.readTree("""{"schemaVersion":1,"name":"Jane Doe"}""") as ObjectNode

schema(document) {
    migration(1, 2) {
        move("/name") to "/fullName"
        add("/enabled") with BooleanNode.TRUE
    }
}
// document is now {"schemaVersion":2,"fullName":"Jane Doe","enabled":true}
```

Declare every migration once; the engine reads the document's current version, skips the steps it has
already applied, and runs the rest up to the latest — see [Getting started](docs/getting-started.md)
and [Concepts](docs/concepts.md).

## Documentation

- [Getting started](docs/getting-started.md) — parse, migrate, and serialize a document end to end.
- [Concepts](docs/concepts.md) — schema, migrations, paths, the safety model, atomic execution, and pending clauses.
- [Operations reference](docs/operations.md) — every DSL operation in detail, with before/after examples.
- [Recipes](docs/recipes.md) — common migration patterns (rename, nest, flatten, merge, split, per-array).
- [Errors](docs/errors.md) — the exception model and a troubleshooting guide.
- [Using json-migrator from Java](docs/using-java.md) — the Java-friendly `JsonMigrator` facade.
- [Internals & design invariants](docs/internals.md) — contributor notes for adding new operations.
- [Architecture decisions](docs/adr/index.md) — ADRs recording significant technical decisions and their reasoning.
- [Contributing](CONTRIBUTING.md) — how to build, the quality gates, and conventions for changes.
- [demo-kotlin](demo-kotlin) — a worked credit-application example (v1→v6) using the Kotlin `schema { }` DSL.
- [demo-java](demo-java) — a worked credit-application example (v1→v6) using the Java `JsonMigrator` facade.

## Core DSL operations

Each operation links to its full description — with before/after examples — in the
[operations reference](docs/operations.md).

| Operation | Use when | Example |
| --- | --- | --- |
| [`add(path) with value`](docs/operations.md#add) | Add a new field. Fails if the field already exists. | `add("/enabled") with BooleanNode.TRUE` |
| [`set(path) with value`](docs/operations.md#set) | Create or overwrite a field. | `set("/enabled") with BooleanNode.TRUE` |
| [`copy(from) to target`](docs/operations.md#copy) | Duplicate a value while keeping the source. | `copy("/id") to "/legacyId"` |
| [`move(from) to target`](docs/operations.md#move) | Rename, nest, flatten, or relocate a value. | `move("/city") to "/address/city"` |
| [`remove(path)`](docs/operations.md#remove) | Delete an existing field. | `remove("/deprecated")` |
| [`merge(sources...) into target`](docs/operations.md#merge) | Join multiple values into one string field. | `merge("/firstName", "/lastName") into "/fullName"` |
| [`split(source).into(targets...)`](docs/operations.md#split) | Split one string field into multiple fields. | `split("/fullName").into("/firstName", "/lastName")` |
| [`forEach(path) { ... }`](docs/operations.md#foreach) | Apply operations to every object in an array. | `forEach("/users") { move("/name") to "/fullName" }` |
| [`createObject(path)`](docs/operations.md#createobject) | Ensure an object exists at a given path. | `createObject("/address")` |
| [`removeIfEmpty(path, cascade)`](docs/operations.md#removeifempty) | Remove an object or array if it becomes empty after migration. | `removeIfEmpty("/address")` |
| [`requireExists(path)`](docs/operations.md#requireexists) | Validate that a required field exists before continuing. | `requireExists("/id")` |
| [`requireType(path, type)`](docs/operations.md#requiretype) | Validate that a value has the expected JSON type. | `requireType("/age", NUMBER)` |
| [`transform(path, lenient) { ... }`](docs/operations.md#transform) | Transform the value at a path using custom logic. | `transform("/age") { IntNode.valueOf(asInt() + 1) }` |
| [`custom { ... }`](docs/operations.md#custom) | Escape hatch for migrations that cannot be expressed with the DSL primitives. | `custom { node -> /* arbitrary Jackson code */ }` |

## Dependency
