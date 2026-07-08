# Using json-migrator from Java

The core migration DSL is written in Kotlin and relies on receiver lambdas, extension functions,
and infix operators that read poorly from Java. The **`json-migrator-engine-java`** module provides
a `JsonMigrator` facade with a fluent, Java-friendly API over the same engine — chainable methods
and `java.util.function` callbacks instead of Kotlin DSL constructs.

## Dependency

Add `json-migrator-engine-java` from Maven Central — same coordinates and version as the engine in
the [README dependency section](../README.md#dependency), with the artifact id
`json-migrator-engine-java`.

This transitively brings in `json-migrator-engine` and Jackson. The library **requires Java 17+**
and **Jackson 3** (see below).

## Jackson 3, not Jackson 2

This library is built on **Jackson 3** (`tools.jackson.*`, group `tools.jackson`), *not* Jackson 2
(`com.fasterxml.jackson`). Your code must use the `tools.jackson` node types (`JsonNode`,
`ObjectNode`, `BooleanNode`, `IntNode`, `StringNode`, …). Note that in Jackson 3 `TextNode` was
renamed to `StringNode`, and `ObjectMapper`/`readTree` throw *unchecked* exceptions.

Create a mapper and parse a document to an `ObjectNode`:

```java
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

JsonMapper mapper = JsonMapper.builder().build();
ObjectNode root = (ObjectNode) mapper.readTree("{\"schemaVersion\":1,\"name\":\"John Doe\"}");
```

## Quick start

```java
import com.mosedotten.json.migrator.engine.java.JsonMigrator;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.ObjectNode;

ObjectNode result = JsonMigrator.migrate(root)
    .migration(1, 2, m -> m
        .merge("/fullName", "/firstName", "/lastName")
        .remove("/deprecated")
        .add("/enabled", BooleanNode.TRUE))
    .migration(2, 3, m -> m
        .transform("/age", v -> IntNode.valueOf(v.asInt() + 1))
        .move("/fullName", "/name"))
    .run();
```

`run()` mutates the node **in place** and also returns it. Each migration runs only when its `from`
matches the document's current version; migrations the document has already outgrown are skipped, so
you can declare the whole pipeline and feed a document at any version. When a migration runs it
applies its operations in order and bumps the version field to `to`.

## Lifecycle

A migration run is built in three stages:

1. **`JsonMigrator.migrate(root)`** — start a chain for a root `ObjectNode`.
2. **Configuration** (optional) — `versionField`, `allowMissingVersionField`, `atomic`/`nonAtomic`/`execution`.
3. **`migration(from, to, steps)`** — one call per version step; `steps` is a
   `Consumer<MigrationSteps>` that records the operations. Repeat to chain versions.
4. **`run()`** — execute all recorded migrations and return the migrated node.

Configuration methods may be called in any order relative to `migration(...)`; the whole chain is
built and executed on `run()`.

## Configuration

| Method | Effect |
| --- | --- |
| `.versionField(String name)` | Version field name (default `"schemaVersion"`). |
| `.allowMissingVersionField()` | Allow a node with no version field; the field is written on the first migration. |
| `.atomic()` | Roll the whole chain back to its starting state if any migration fails (**default**). |
| `.nonAtomic()` | Leave partial mutations in place when a migration fails. |
| `.execution(ExecutionStrategy)` | Supply a custom execution strategy (advanced). |

```java
ObjectNode result = JsonMigrator.migrate(root)
    .versionField("version")
    .allowMissingVersionField()
    .nonAtomic()
    .migration(1, 2, m -> m.add("/enabled", BooleanNode.TRUE))
    .run();
```

## Operations

All operation methods live on `MigrationSteps` (the `m` in `migration(from, to, m -> …)`), are
chainable, and return the same `MigrationSteps`.

| Method | Description |
| --- | --- |
| `add(path, value)` | Add a new field. Fails if it already exists. |
| `set(path, value)` | Create or overwrite a field. |
| `copy(from, to)` | Duplicate a value, keeping the source. |
| `move(from, to)` | Rename / relocate a value. |
| `remove(path)` | Delete an existing field. |
| `merge(target, sources...)` | Join multiple string values into one target field (space-separated). |
| `merge(target, joiner, sources...)` | As above, with a custom `ValueJoinerStrategy`. |
| `split(source, targets...)` | Split one string field into multiple targets (space-separated). |
| `split(source, splitter, targets...)` | As above, with a custom `ValueSplitterStrategy`. |
| `createObject(path)` | Ensure an (empty) object exists at a path. |
| `removeIfEmpty(path)` | Remove a field if it is an empty object/array. |
| `removeIfEmpty(path, cascade)` | As above; with `cascade = true`, also remove parents left empty. |
| `requireExists(path)` | Assert a field exists (fails the migration otherwise). |
| `requireType(path, JsonType)` | Assert a field is of a given `JsonType` (`STRING`, `NUMBER`, `BOOLEAN`, `OBJECT`, `ARRAY`, `NULL`). |
| `transform(path, fn)` | Replace a value via a `Function<JsonNode, JsonNode>`. Fails if the field is missing. |
| `transformLenient(path, fn)` | Like `transform`, but a no-op when the field is missing. |
| `custom(block)` | Run an arbitrary `Consumer<ObjectNode>` against the root node. |
| `forEach(path, steps)` | Apply nested operations to every object in an array. |

Paths are [JSON Pointer](https://datatracker.ietf.org/doc/html/rfc6901)-style: `/field`,
`/parent/child`. Use `~1` for a literal `/` and `~0` for a literal `~` in a field name.

### Lambdas: transform, custom, forEach

```java
// transform: derive a new value from the current one
m.transform("/age", v -> IntNode.valueOf(v.asInt() + 1));

// custom: arbitrary mutation of the root node (escape hatch)
m.custom(node -> {
    node.set("id", node.get("legacyId"));
    node.remove("legacyId");
});

// forEach: apply nested operations to each element of an array
m.forEach("/users", u -> u
    .move("/name", "/fullName")
    .add("/active", BooleanNode.TRUE));
```

### Custom join/split strategies

`ValueJoinerStrategy` and `ValueSplitterStrategy` are functional interfaces, so you can pass Java
lambdas:

```java
import com.mosedotten.json.migrator.engine.operation.ValueJoinerStrategy;
import com.mosedotten.json.migrator.engine.operation.ValueSplitterStrategy;
import java.util.List;
import java.util.stream.Collectors;

ValueJoinerStrategy commaJoiner =
    values -> values.stream().map(JsonNode::asString).collect(Collectors.joining(","));
ValueSplitterStrategy commaSplitter =
    value -> List.of(value.asString().split(","));

m.merge("/fullName", commaJoiner, "/firstName", "/lastName");
m.split("/fullName", commaSplitter, "/firstName", "/lastName");
```

## Chaining multiple migrations

Each `migration(from, to, …)` is applied in the order declared, advancing the version one step at a
time. The engine picks up the document at whatever version it declares and skips the steps it has
already passed, so declaring the full pipeline and feeding a partially-migrated document is safe:

```java
ObjectNode result = JsonMigrator.migrate(root)   // root at schemaVersion 1
    .migration(1, 2, m -> m.add("/enabled", BooleanNode.TRUE))
    .migration(2, 3, m -> m.add("/verified", BooleanNode.TRUE))
    .run();                                       // result at schemaVersion 3
```

A document already at `schemaVersion 2` above would skip the `1 -> 2` step and run only `2 -> 3`; a
document already at (or beyond) the last version is returned untouched. A document whose version sits
*before* the first applicable step (a forward gap) raises `MigrationVersionException`.

Adjacent versions only (`|from - to| == 1`); `0` is not a valid version. Downgrades
(e.g. `migration(2, 1)`) are permitted.

## Error handling

All failures are unchecked and extend `com.mosedotten.json.migrator.engine.exception.MigrationException`
(a `RuntimeException`), so nothing forces a `try/catch`.

- **`MigrationVersionException`** — the document's version sits before an applicable migration (a
  forward gap), the version field is missing (and not opted in) or non-integer, a version is `0`, or
  `from`/`to` are not adjacent.
- **`MigrationExecutionException`** — an operation failed while applying a migration. It wraps the
  underlying cause and carries context:

  ```java
  import com.mosedotten.json.migrator.engine.exception.MigrationExecutionException;

  try {
      JsonMigrator.migrate(root)
          .migration(1, 2, m -> m.add("/name", IntNode.valueOf(30))) // field already exists
          .run();
  } catch (MigrationExecutionException e) {
      e.getFromVersion();          // 1
      e.getToVersion();            // 2
      e.getOperationIndex();       // 1 (1-based)
      e.getOperationDescription(); // add("/name")
      e.getFailure();              // the underlying ExistingFieldException
  }
  ```

The underlying causes are also `MigrationException` subtypes — `MissingFieldException`,
`ExistingFieldException`, `InvalidFieldTypeException`, `InvalidFieldValueException`,
`InvalidJsonPathException`, `InvalidOperationException` — each carrying a descriptive message
(field-related ones also expose `getPath()`).

By default execution is **atomic**: if any migration fails, the root node is restored to its
original state before the exception propagates. Use `.nonAtomic()` to keep partial mutations.

## Full example

```java
import com.mosedotten.json.migrator.engine.java.JsonMigrator;
import com.mosedotten.json.migrator.engine.operation.JsonType;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.ObjectNode;

public class Example {
    public static void main(String[] args) {
        JsonMapper mapper = JsonMapper.builder().build();
        ObjectNode root = (ObjectNode) mapper.readTree(
            "{\"schemaVersion\":1,\"firstName\":\"John\",\"lastName\":\"Doe\","
                + "\"deprecated\":true,\"age\":30,\"users\":[{\"name\":\"A\"},{\"name\":\"B\"}]}");

        ObjectNode result = JsonMigrator.migrate(root)
            .migration(1, 2, m -> m
                .requireType("/age", JsonType.NUMBER)
                .merge("/fullName", "/firstName", "/lastName")
                .remove("/deprecated")
                .add("/enabled", BooleanNode.TRUE)
                .forEach("/users", u -> u.move("/name", "/fullName")))
            .migration(2, 3, m -> m
                .transform("/age", v -> IntNode.valueOf(v.asInt() + 1))
                .move("/fullName", "/name"))
            .run();

        System.out.println(result.toPrettyString());
    }
}
```

Output:

```json
{
  "schemaVersion" : 3,
  "age" : 31,
  "users" : [ { "fullName" : "A" }, { "fullName" : "B" } ],
  "enabled" : true,
  "name" : "John Doe"
}
```
