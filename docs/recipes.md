# Recipes

## Rename a field

Use `move` when a field has a new name and the old field should disappear.

```kotlin
migration(1, 2) {
    move("/name") to "/fullName"
}
```

Before:

```json
{"schemaVersion":1,"name":"Jane Doe"}
```

After:

```json
{"schemaVersion":2,"fullName":"Jane Doe"}
```

## Add a default value

Use `add` when the field must not already exist.

```kotlin
migration(1, 2) {
    add("/enabled") with BooleanNode.TRUE
}
```

Use `set` instead when existing values should be overwritten.

## Move a flat field into an object

Target parent objects are created when needed.

```kotlin
migration(1, 2) {
    move("/city") to "/address/city"
}
```

Before:

```json
{"schemaVersion":1,"city":"Oslo"}
```

After:

```json
{"schemaVersion":2,"address":{"city":"Oslo"}}
```

## Flatten a nested field

Move from a nested path to a top-level path.

```kotlin
migration(1, 2) {
    move("/address/city") to "/city"
}
```

Before:

```json
{"schemaVersion":1,"address":{"city":"Oslo"}}
```

After:

```json
{"schemaVersion":2,"address":{},"city":"Oslo"}
```

The empty parent object is left in place. Remove it explicitly if the new schema should not keep it.

## Combine fields

Use `merge` when separate fields become one display field.

```kotlin
migration(1, 2) {
    merge("/firstName", "/lastName") into "/fullName"
}
```

Before:

```json
{"schemaVersion":1,"firstName":"Jane","lastName":"Doe"}
```

After:

```json
{"schemaVersion":2,"fullName":"Jane Doe"}
```

## Split a field

Use `split` when one space-separated value becomes multiple fields.

```kotlin
migration(1, 2) {
    split("/fullName").into("/firstName", "/lastName")
}
```

Before:

```json
{"schemaVersion":1,"fullName":"Jane Doe"}
```

After:

```json
{"schemaVersion":2,"firstName":"Jane","lastName":"Doe"}
```

The number of space-separated pieces must exactly match the number of targets.

## Migrate every object in an array

Use `forEach` when an operation should run inside each array element.

```kotlin
migration(1, 2) {
    forEach("/users") {
        move("/name") to "/fullName"
    }
}
```

Before:

```json
{"schemaVersion":1,"users":[{"name":"Jane"},{"name":"John"}]}
```

After:

```json
{"schemaVersion":2,"users":[{"fullName":"Jane"},{"fullName":"John"}]}
```

## Chain multiple versions

Declare one adjacent step per migration.

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

The engine runs each step whose `from` matches the document's current version and skips ones it has
already passed, so the chain migrates a document from its current version up to the latest.
