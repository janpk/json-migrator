# Errors

The engine fails fast when a migration is invalid or cannot be applied to the current document.
Use these failures as test feedback: either the migration definition is incomplete, or the input
document does not match the expected source schema.

## Exception model

Operation failures use typed exceptions from `com.mosedotten.json.migrator.engine.exception`.
When an operation fails inside a `migration(from, to)` block, the engine wraps the root cause in
`MigrationExecutionException` so callers can see where the failure occurred.

```kotlin
try {
    schema(rootNode) {
        migration(1, 2) {
            add("/name") with StringNode.valueOf("Jane")
        }
    }
} catch (exception: MigrationExecutionException) {
    println(exception.fromVersion)
    println(exception.toVersion)
    println(exception.operationIndex)
    println(exception.operationDescription)
    println(exception.failure)
}
```

Common exception types:

| Exception | Meaning |
| --- | --- |
| `MigrationExecutionException` | A migration operation failed; inspect metadata and `cause`. |
| `MissingFieldException` | A required source field or operation target path was missing. |
| `ExistingFieldException` | An operation tried to create a field that already exists. |
| `InvalidFieldTypeException` | A field existed but had the wrong JSON type. |
| `InvalidFieldValueException` | A field value could not be transformed as requested. |
| `InvalidOperationException` | An operation was configured with invalid arguments. |
| `InvalidJsonPathException` | A path string was malformed; thrown when the operation is built, not during execution. |
| `IncompleteDslClauseException` | A DSL clause such as `add("/x")` was not completed. |
| `DslClauseAlreadyCompletedException` | A pending DSL clause was completed more than once. |
| `MigrationVersionException` | A version problem: a forward gap (no migration advances the document to a step's `from`), a missing version field (and `allowNoVersionField` was not set), a non-integer version field, a `0` version, or non-adjacent `from`/`to`. |

## Version errors

| Symptom | Cause | Fix |
| --- | --- | --- |
| `from must not be 0` | A migration starts at version `0`. | Start at a non-zero version. |
| `to must not be 0` | A migration targets version `0`. | Target a non-zero version. |
| `from and to must be adjacent versions` | A migration skips versions. | Split it into adjacent steps, such as `1 -> 2` and `2 -> 3`. |
| `root node must contain version field 'schemaVersion'` | The document has no configured version field. | Add the field or pass `allowNoVersionField = true` for bootstrap migrations. |
| `version field 'schemaVersion' must be an integer` | The version field is present but not a JSON integer (e.g. the string `"1"`). | Store the version as an integer. |
| `root node version field 'schemaVersion' is 5; no migration advances it to from version 6` | The document's version is *before* the first step that could pick it up — a gap in the declared chain. | Add the missing adjacent step(s) so the chain reaches the document's version. |

The engine migrates a document from its current version up to the latest declared step: steps the
document has already passed are skipped, and a document already at (or beyond) the last version is
returned unchanged rather than raising an error. Only a genuine gap — a version no declared step can
advance — fails.

## Path errors

Every operation parses its path when it is constructed, so a malformed path fails with
`InvalidJsonPathException` as the migration is being built — before any document is touched, and
without being wrapped in `MigrationExecutionException`. Paths use JSON Pointer syntax (RFC 6901),
where `~1` escapes a literal `/` and `~0` escapes a literal `~`.

| Symptom | Cause | Fix |
| --- | --- | --- |
| `Invalid JSON path '': path must point to a field, not the document root` | The path is empty. | Point to a field, such as `/name`. |
| `Invalid JSON path 'name': path must start with '/'` | The path has no leading slash. | Prefix the path with `/`, such as `/name`. |
| `Invalid JSON path '/bad~2path': escape sequences must be '~0' for '~' or '~1' for '/'` | The path uses an unknown `~` escape. | Escape literals as `~1` for `/` and `~0` for `~`, e.g. `/a~1b` for a field named `a/b`. |

## Incomplete DSL clauses

These calls are incomplete by themselves:

```kotlin
add("/enabled")
set("/enabled")
move("/name")
copy("/id")
merge("/firstName", "/lastName")
split("/fullName")
```

Complete them with the required second part:

```kotlin
add("/enabled") with BooleanNode.TRUE
set("/enabled") with BooleanNode.TRUE
move("/name") to "/fullName"
copy("/id") to "/legacyId"
merge("/firstName", "/lastName") into "/fullName"
split("/fullName").into("/firstName", "/lastName")
```

Nested incomplete clauses inside `forEach` fail for the same reason.

## Operation errors

| Operation | Common failure | Fix |
| --- | --- | --- |
| `add` | Target field already exists. | Use `set` if overwriting is intended, or guard the input before migration. |
| `copy` | Source field is missing. | Confirm the input version or use a migration step that creates the source first. |
| `copy` | Target field already exists. | Choose a new target or remove/set the target intentionally. |
| `move` | Source field is missing or target exists. | Fix the path or split the migration into explicit remove/set behavior. |
| `remove` | Field does not exist. | Only remove fields guaranteed by the source schema. |
| `merge` | Fewer than two sources are supplied. | Use at least two source paths. |
| `merge` | A source is missing or target exists. | Fix the input schema expectation or target path. |
| `split` | Fewer than two targets are supplied. | Supply at least two target paths. |
| `split` | Piece count does not match target count. | Adjust the input format or target count. |
| `forEach` | Path is not an array. | Point to an array field. |
| `forEach` | An array element is not an object. | Normalize the array before using object-field operations. |

## Debugging checklist

- Confirm the input JSON has the expected `schemaVersion`.
- Confirm every migration step is adjacent.
- Confirm paths start with `/` and point to object fields.
- Confirm field names containing `/` or `~` are escaped as `~1` and `~0`.
- Confirm target paths do not already exist for `add`, `copy`, `move`, `merge`, and `split`.
- Confirm source paths exist for `copy`, `move`, `remove`, `merge`, and `split`.
