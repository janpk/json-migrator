# Concepts

## Schema

`schema(rootNode) { ... }` is the entry point. It receives a Jackson `ObjectNode` and returns the
same node after applying the configured migrations.

The engine mutates the supplied node in place. If callers need to keep the original document, copy
or reparse the JSON before passing it to `schema`.

## Migrations

A migration defines one adjacent version step:

```kotlin
migration(1, 2) {
    move("/name") to "/fullName"
}
```

Version rules:

- `from` and `to` must be adjacent versions.
- Version `0` is not valid.
- A migration runs only when the document's current version equals its `from`. Steps the document
  has already passed are skipped, and the configured version field is set to `to` after each step
  that runs.

Migrations can be chained:

```kotlin
schema(rootNode) {
    migration(1, 2) {
        move("/name") to "/fullName"
    }
    migration(2, 3) {
        add("/enabled") with BooleanNode.TRUE
    }
}
```

The engine reads the document's current version, skips the migrations it has already applied, and
runs the rest through to the last declared step. So you declare the whole chain once and feed a
document at any version: a v1 document runs every step, a v2 document resumes at `2 -> 3`, and a
document already at (or beyond) the last version is returned unchanged. If a document's version sits
*before* the first step that could advance it — a gap in the chain — the run fails with a
`MigrationVersionException`.

## Paths

Paths use JSON Pointer style object paths:

```text
/name
/address/city
/metadata/externalId
```

Escaping follows JSON Pointer conventions:

- `~1` represents `/`
- `~0` represents `~`

Operations currently navigate object fields. Use `forEach("/arrayPath") { ... }` when applying
field operations to objects inside an array.

## Safety model

Operations fail fast with exceptions when the requested transformation would be unsafe or
ambiguous. Examples:

- `add` fails if the target field already exists.
- `copy` and `move` fail if the source does not exist or the target already exists.
- `remove` fails if the field does not exist.
- `split` fails if the number of produced pieces does not match the number of targets.

This makes migrations explicit and helps catch schema drift during tests or application startup.

## Atomic execution

By default, execution is atomic: the engine snapshots the document before running and, if any
operation or migration fails, restores the original document in place before rethrowing. A failure in
a later migration therefore rolls back earlier successful migrations, so the node is never left in a
partially migrated state.

This is controlled by the `execution` parameter on `schema(...)`, which takes an `ExecutionStrategy`:

```kotlin
import com.mosedotten.json.migrator.engine.dsl.ExecutionStrategy

// Default: roll back the document on any failure.
schema(rootNode, execution = ExecutionStrategy.Atomic) { /* ... */ }

// Opt out: apply operations directly and leave partial mutations on failure.
schema(rootNode, execution = ExecutionStrategy.NonAtomic) { /* ... */ }
```

`ExecutionStrategy.NonAtomic` skips the snapshot and leaves whatever was applied before the failure in
place. Prefer the default `Atomic` unless you have a specific reason to inspect partial results.

## Pending clauses

Some DSL calls are intentionally incomplete until they receive their second part:

```kotlin
add("/enabled") with BooleanNode.TRUE
move("/name") to "/fullName"
merge("/firstName", "/lastName") into "/fullName"
split("/fullName").into("/firstName", "/lastName")
```

Leaving a clause incomplete fails when the migration is built:

```kotlin
migration(1, 2) {
    split("/fullName")
}
```

This protects against accidentally declaring a no-op migration.
