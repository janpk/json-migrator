# json-migrator - migration of JSON structures

![](https://github.com/janpk/json-migrator/raw/metrics/badges/lines.svg)
![](https://github.com/janpk/json-migrator/raw/metrics/badges/branches.svg)
![](https://github.com/janpk/json-migrator/raw/metrics/badges/test_ratio.svg)

[Full metrics dashboard](https://github.com/janpk/json-migrator/blob/metrics/METRICS.md)

`json-migrator` is a Kotlin DSL for transforming JSON documents between schema versions.
It applies explicit migrations to a Jackson `ObjectNode`, updates the configured version field,
and fails fast when a migration cannot be applied safely.

## Quick start

## Documentation

## Core DSL operations

| Operation | Use when | Example |
| --- | --- | --- |
| `add(path) with value` | Add a new field. Fails if the field already exists. | `add("/enabled") with BooleanNode.TRUE` |
| `set(path) with value` | Create or overwrite a field. | `set("/enabled") with BooleanNode.TRUE` |
| `copy(from) to target` | Duplicate a value while keeping the source. | `copy("/id") to "/legacyId"` |
| `move(from) to target` | Rename, nest, flatten, or relocate a value. | `move("/city") to "/address/city"` |
| `remove(path)` | Delete an existing field. | `remove("/deprecated")` |
| `merge(sources...) into target` | Join multiple values into one string field. | `merge("/firstName", "/lastName") into "/fullName"` |

## Dependency
