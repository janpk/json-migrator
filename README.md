# json-migrator - migration of JSON structures

| **Tests** | ![](https://github.com/janpk/json-migrator/raw/metrics/badges/test_results.svg) ![](https://github.com/janpk/json-migrator/raw/metrics/badges/test_classes.svg) ![](https://github.com/janpk/json-migrator/raw/metrics/badges/test_cases.svg) ![](https://github.com/janpk/json-migrator/raw/metrics/badges/test_skipped.svg) ![](https://github.com/janpk/json-migrator/raw/metrics/badges/test_ratio.svg) |
| --- | --- |
| **Coverage** | ![](https://github.com/janpk/json-migrator/raw/metrics/badges/lines.svg) ![](https://github.com/janpk/json-migrator/raw/metrics/badges/branches.svg) ![](https://github.com/janpk/json-migrator/raw/metrics/badges/classes.svg) ![](https://github.com/janpk/json-migrator/raw/metrics/badges/instructions.svg) ![](https://github.com/janpk/json-migrator/raw/metrics/badges/methods.svg) |

[Full metrics dashboard](https://github.com/janpk/json-migrator/blob/metrics/METRICS.md)

`json-migrator` is a Kotlin DSL for transforming JSON documents between schema versions.
It applies explicit migrations to a Jackson `ObjectNode`, updates the configured version field,
and fails fast when a migration cannot be applied safely.

## Quick start

## Documentation

- [Using json-migrator from Java](docs/using-java.md) — the Java-friendly `JsonMigrator` facade.
- [demo-kotlin](demo-kotlin) — a worked credit-application example (v1→v6) using the Kotlin `schema { }` DSL.
- [demo-java](demo-java) — a worked credit-application example (v1→v6) using the Java `JsonMigrator` facade.

## Core DSL operations

| Operation | Use when | Example |
| --- | --- | --- |
| `add(path) with value` | Add a new field. Fails if the field already exists. | `add("/enabled") with BooleanNode.TRUE` |
| `set(path) with value` | Create or overwrite a field. | `set("/enabled") with BooleanNode.TRUE` |
| `copy(from) to target` | Duplicate a value while keeping the source. | `copy("/id") to "/legacyId"` |
| `move(from) to target` | Rename, nest, flatten, or relocate a value. | `move("/city") to "/address/city"` |
| `remove(path)` | Delete an existing field. | `remove("/deprecated")` |
| `merge(sources...) into target` | Join multiple values into one string field. | `merge("/firstName", "/lastName") into "/fullName"` |
| `split(source).into(targets...)` | Split one string field into multiple fields. | `split("/fullName").into("/firstName", "/lastName")` |
| `forEach(path) { ... }` | Apply operations to every object in an array. | `forEach("/users") { move("/name") to "/fullName" }` |
| `createObject(path)` | Ensure an object exists at a given path. | `createObject("/address")` |
| `removeIfEmpty(path, cascade)` | Remove an object or array if it becomes empty after migration. | `removeIfEmpty("/address")` |
| `requireExists(path)` | Validate that a required field exists before continuing. | `requireExists("/id")` |
| `requireType(path, type)` | Validate that a value has the expected JSON type. | `requireType("/age", NUMBER)` |
| `transform(path, lenient) { ... }` | Transform the value at a path using custom logic. | `transform("/age") { IntNode.valueOf(asInt() + 1) }` |
| `custom { ... }`| Escape hatch for migrations that cannot be expressed with the DSL primitives. | `custom { node -> /* arbitrary Jackson code */ }` |

## Dependency
