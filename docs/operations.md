# Operations reference

## Table of contents

- [`add(path) with value`](#add)
- [`set(path) with value`](#set)
- [`copy(from) to target`](#copy)
- [`move(from) to target`](#move)
- [`remove(path)`](#remove)
- [`merge(sources...) into target`](#merge)
- [`split(source).into(targets...)`](#split)
- [`forEach(path) { ... }`](#foreach)
- [`createObject(path)`](#createobject)
- [`removeIfEmpty(path, cascade)`](#removeifempty)
- [`requireExists(path)`](#requireexists)
- [`requireType(path, type)`](#requiretype)
- [`transform(path, lenient) { ... }`](#transform)
- [`custom { ... }`](#custom)

All examples assume this wrapper unless shown otherwise:

```kotlin
schema(rootNode) {
    migration(1, 2) {
        // operation goes here
    }
}
```

<a id="add"></a>

## `add(path) with value`

Use `add` to create a field that must not already exist.

```kotlin
add("/enabled") with BooleanNode.TRUE
```

Before:

```json
{"schemaVersion":1,"name":"Jane"}
```

After:

```json
{"schemaVersion":2,"name":"Jane","enabled":true}
```

Rules:

- Creates missing parent objects.
- Fails if the target field already exists.
- Requires `with <JsonNode>`.

<a id="set"></a>

## `set(path) with value`

Use `set` to create or overwrite a field.

```kotlin
set("/enabled") with BooleanNode.TRUE
```

Before:

```json
{"schemaVersion":1,"enabled":false}
```

After:

```json
{"schemaVersion":2,"enabled":true}
```

Rules:

- Creates missing parent objects.
- Overwrites existing values.
- Requires `with <JsonNode>`.

<a id="copy"></a>

## `copy(from) to target`

Use `copy` to duplicate a value while keeping the original field.

```kotlin
copy("/id") to "/legacyId"
```

Before:

```json
{"schemaVersion":1,"id":"123"}
```

After:

```json
{"schemaVersion":2,"id":"123","legacyId":"123"}
```

Rules:

- Fails if the source field does not exist.
- Fails if the target field already exists.
- Creates missing parent objects for the target path.
- Requires `to <target>`.

<a id="move"></a>

## `move(from) to target`

Use `move` to rename, nest, flatten, or relocate a value.

```kotlin
move("/city") to "/address/city"
```

Before:

```json
{"schemaVersion":1,"city":"Oslo"}
```

After:

```json
{"schemaVersion":2,"address":{"city":"Oslo"}}
```

Rules:

- Fails if the source field does not exist.
- Fails if the target field already exists.
- Creates missing parent objects for the target path.
- Removes the source after copying the value.
- Requires `to <target>`.

<a id="remove"></a>

## `remove(path)`

Use `remove` to delete an existing field.

```kotlin
remove("/deprecated")
```

Before:

```json
{"schemaVersion":1,"name":"Jane","deprecated":true}
```

After:

```json
{"schemaVersion":2,"name":"Jane"}
```

Rules:

- Fails if the field does not exist.
- Removes the field from its parent object.

<a id="merge"></a>

## `merge(sources...) into target`

Use `merge` to combine multiple values into one string field.

```kotlin
merge("/firstName", "/lastName") into "/fullName"
```

Before:

```json
{"schemaVersion":1,"firstName":"Jane","lastName":"Doe"}
```

After:

```json
{"schemaVersion":2,"fullName":"Jane Doe"}
```

Rules:

- Requires at least two source paths.
- Composes the source values with a `ValueJoinerStrategy` (default: read each with `JsonNode.asString()` and join with a single space).
- Adds the target field, then removes all source fields.
- Fails if any source field does not exist.
- Fails if the target field already exists.
- Requires `into <target>`.

### Custom formatting

The default joins source values with a single space. To change the formatting, pass a `ValueJoinerStrategy`:

```kotlin
val commaJoiner = ValueJoinerStrategy { values -> values.joinToString(",") { it.asString() } }

merge("/firstName", "/lastName", joiner = commaJoiner) into "/fullName"   // "Jane,Doe"
```

A `ValueJoinerStrategy` receives the source values (as `JsonNode`s) and returns the composed string, so it owns both how each value is read and how they are combined. The default is `ValueJoinerStrategy.SpaceSeparated`.

<a id="split"></a>

## `split(source).into(targets...)`

Use `split` to divide one string field into multiple fields.

```kotlin
split("/fullName").into("/firstName", "/lastName")
```

Before:

```json
{"schemaVersion":1,"fullName":"Jane Doe"}
```

After:

```json
{"schemaVersion":2,"firstName":"Jane","lastName":"Doe"}
```

Rules:

- Requires at least two target paths.
- Divides the source value with a `ValueSplitterStrategy` (default: read with `JsonNode.asString()` and split on spaces).
- Fails if the number of pieces does not match the number of targets.
- Adds all target fields, then removes the source field.
- Fails if the source field does not exist.
- Fails if any target field already exists.
- Requires `.into(<targets>)`.

### Custom formatting

The default splits the source value on spaces. To change the formatting, pass a `ValueSplitterStrategy`:

```kotlin
val commaSplitter = ValueSplitterStrategy { value -> value.asString().split(",") }

split("/fullName", splitter = commaSplitter).into("/firstName", "/lastName")   // "Jane,Doe"
```

A `ValueSplitterStrategy` receives the source value (as a `JsonNode`) and returns the pieces, so it owns both how the value is read and how it is divided. The default is `ValueSplitterStrategy.SpaceSeparated`. `merge` and `split` use matching defaults, so a `split` followed by a `merge` round-trips.

<a id="foreach"></a>

## `forEach(path) { ... }`

Use `forEach` to apply operations to every object in an array.

```kotlin
forEach("/users") {
    move("/name") to "/fullName"
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

Rules:

- The path must point to an array.
- Every array element must be an object.
- Nested paths are relative to each array element.
- Nested incomplete clauses fail when the migration is built.

<a id="createobject"></a>

## `createObject(path)`

Use `createObject` to ensure an object exists at a given path.

```kotlin
createObject("/address")
```

Before:

```json
{"schemaVersion":1,"name":"Jane"}
```

After:

```json
{"schemaVersion":2,"name":"Jane","address":{}}
```

Rules:

- Creates an empty object `{}` at the path if it does not exist.
- Is a no-op if the path already contains an object.
- Creates missing parent objects as needed.
- Fails if the path exists but is not an object (scalar, array, null).
- Fails if any ancestor exists but is not an object.

<a id="removeifempty"></a>

## `removeIfEmpty(path, cascade)`

Use `removeIfEmpty` to delete a field only if it contains an empty object or empty array. Without `cascade`, it stops when the target is removed. With `cascade = true`, it recursively removes parent objects that become empty.

```kotlin
removeIfEmpty("/address")
removeIfEmpty("/items", cascade = true)
```

Before:

```json
{"schemaVersion":1,"name":"Jane","address":{}}
```

After:

```json
{"schemaVersion":2,"name":"Jane"}
```

### Without cascade (default)

```json
{
  "before": {"profile": {"contact": {}}, "name": "John"},
  "operation": "removeIfEmpty(\"/profile/contact\")",
  "after": {"profile": {}, "name": "John"}
}
```

The empty `contact` object is removed, but the now-empty `profile` parent remains.

### With cascade = true

```json
{
  "before": {"profile": {"contact": {}}, "name": "John"},
  "operation": "removeIfEmpty(\"/profile/contact\", cascade = true)",
  "after": {"name": "John"}
}
```

The empty `contact` object is removed, and then the now-empty `profile` parent is also removed.

### Cascade stops at non-empty parents

```json
{
  "before": {"profile": {"contact": {}, "age": 30}, "name": "John"},
  "operation": "removeIfEmpty(\"/profile/contact\", cascade = true)",
  "after": {"profile": {"age": 30}, "name": "John"}
}
```

The empty `contact` object is removed, but `profile` is kept because it still contains `age`.

Rules:

- Removes the field if it is an empty object (`{}`) or empty array (`[]`).
- Is a no-op if the field does not exist (lenient behavior).
- Is a no-op if the field is a scalar (string, number, boolean) or null (lenient behavior).
- Is a no-op if the field is a non-empty container (has any properties or elements).
- Objects with only null values are not considered empty (not `{}`).
- Arrays with only null elements are not considered empty (not `[]`).
- When `cascade = false` (default), stops after removing the target field.
- When `cascade = true`, recursively removes parent objects if they become empty, stopping at the first non-empty parent or at the document root.

<a id="requireexists"></a>

## `requireExists(path)`

Use `requireExists` to assert that a required field is present before continuing. It is a validation guard: it never modifies the document, it either passes silently or fails the migration.

```kotlin
requireExists("/id")
```

Before (passes, document unchanged):

```json
{"schemaVersion":1,"id":"123","name":"Jane"}
```

After:

```json
{"schemaVersion":2,"id":"123","name":"Jane"}
```

When the field is missing, the migration fails:

```json
{
  "before": {"schemaVersion": 1, "name": "Jane"},
  "operation": "requireExists(\"/id\")",
  "result": "fails: Required field '/id' does not exist"
}
```

Rules:

- Passes if the field exists at the path, leaving the document unchanged.
- Existence is key presence only: a field explicitly set to `null` passes.
- Empty containers (`{}`, `[]`) and falsy scalars (`false`, `0`) pass, because the field is present.
- Fails if the field is missing.
- Fails if any intermediate ancestor is missing or is not an object (scalar, array).

<a id="requiretype"></a>

## `requireType(path, type)`

Use `requireType` to assert that a value has an expected JSON type before continuing. Like `requireExists`, it is a validation guard: it never modifies the document, it either passes silently or fails the migration.

The `type` is a `JsonType`: `STRING`, `NUMBER`, `BOOLEAN`, `OBJECT`, `ARRAY`, or `NULL`. `NUMBER` matches both integers and decimals.

```kotlin
requireType("/age", JsonType.NUMBER)
```

Before (passes, document unchanged):

```json
{"schemaVersion":1,"age":30}
```

After:

```json
{"schemaVersion":2,"age":30}
```

When the value has the wrong type, the migration fails:

```json
{
  "before": {"schemaVersion": 1, "age": "30"},
  "operation": "requireType(\"/age\", JsonType.NUMBER)",
  "result": "fails: Field '/age' is not of type NUMBER; actual type was StringNode"
}
```

Rules:

- Passes if the value at the path matches the expected type, leaving the document unchanged.
- `NUMBER` matches both integers and decimals.
- `OBJECT` and `ARRAY` are distinct: an array does not satisfy `OBJECT`, and vice versa.
- Empty containers still match: `{}` satisfies `OBJECT`, `[]` satisfies `ARRAY`.
- A `null` value matches only `NULL`; every other type fails on `null`, and `NULL` fails on any non-null value.
- Fails if the field is missing (validate presence explicitly with [`requireExists`](#requireexists) if needed).
- Fails if any intermediate ancestor is missing or is not an object (scalar, array).

<a id="transform"></a>

## `transform(path, lenient) { ... }`

Use `transform` to replace the value at a path with the result of custom logic. The lambda receives the current value as `this` (a `JsonNode`) and returns its replacement `JsonNode`. The value is replaced in place.

```kotlin
transform("/age") { IntNode.valueOf(asInt() + 1) }
```

Before:

```json
{"schemaVersion":1,"age":30}
```

After:

```json
{"schemaVersion":2,"age":31}
```

The replacement may be a different JSON type, so `transform` can also convert values:

```kotlin
transform("/age") { StringNode.valueOf(asInt().toString()) }   // 30 -> "30"
```

By default a missing field fails the migration. Pass `lenient = true` to make a missing field a no-op instead:

```kotlin
transform("/age", lenient = true) { IntNode.valueOf(asInt() + 1) }
```

```json
{
  "before": {"schemaVersion": 1, "name": "Jane"},
  "operation": "transform(\"/age\") { ... }",
  "result": "fails: Field '/age' does not exist"
}
```

Rules:

- Replaces the value at the path with whatever the lambda returns, leaving sibling fields untouched.
- The lambda receiver is the current value; the returned node may be any JSON type (including a different type, or `NullNode` to set null).
- A field explicitly set to `null` is present, so the lambda runs with a `NullNode` receiver.
- Fails if the field is missing, unless `lenient = true`, in which case a missing field is a no-op.
- With `lenient = false` (default), also fails if any intermediate ancestor is missing or is not an object.

<a id="custom"></a>

## `custom { ... }`

Use `custom` as an escape hatch for migrations that cannot be expressed with the DSL primitives. The block receives the document `ObjectNode` and may mutate it with arbitrary Jackson code. Prefer the dedicated operations where they fit; reach for `custom` only when they do not.

```kotlin
custom { node ->
    node.set("fullName", node.get("name"))
    node.remove("name")
}
```

Before:

```json
{"schemaVersion":1,"name":"Jane"}
```

After:

```json
{"schemaVersion":2,"fullName":"Jane"}
```

Rules:

- The block receives the document node and mutates it in place; whatever it returns is ignored.
- There are no built-in guards: validation and error handling are the block's responsibility.
- Exceptions thrown by the block propagate and fail the migration.
- Multiple operations (including several `custom` blocks) apply in the order they are declared, so later blocks see earlier changes.
