# Getting started

This guide shows the smallest complete flow: parse JSON into a Jackson `ObjectNode`, run a schema
migration, and serialize the migrated result.

## Add the dependency

Add `json-migrator-engine` from Maven Central — see the coordinates and current version in the
[README dependency section](../README.md#dependency).

The engine is a Kotlin/JVM library and uses Jackson 3 `tools.jackson` types. It requires **Java 17+**
and **Jackson 3**, and is usable from **Kotlin 2.0+**.

## Run a migration

```kotlin
import com.mosedotten.json.migrator.engine.dsl.clause.add
import com.mosedotten.json.migrator.engine.dsl.clause.move
import com.mosedotten.json.migrator.engine.dsl.schema
import tools.jackson.databind.node.BooleanNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.jacksonObjectMapper

fun main() {
    val mapper = jacksonObjectMapper()
    val input = """{"schemaVersion":1,"name":"Jane Doe"}"""
    val rootNode = mapper.readTree(input) as ObjectNode

    val migrated = schema(rootNode) {
        migration(1, 2) {
            move("/name") to "/fullName"
            add("/enabled") with BooleanNode.TRUE
        }
    }

    println(mapper.writeValueAsString(migrated))
}
```

Output:

```json
{"schemaVersion":2,"fullName":"Jane Doe","enabled":true}
```

## Chain migrations

Each `migration(from, to)` block runs against the same root node in declaration order. The engine
applies a step only when the document's current version matches its `from`, skips steps the document
has already passed, and advances the version field to `to` after each step that runs. So the same
chain carries a document from whatever version it is at up to the latest.

```kotlin
schema(rootNode) {
    migration(1, 2) {
        move("/name") to "/fullName"
    }
    migration(2, 3) {
        add("/contact/verified") with BooleanNode.FALSE
    }
}
```

This migrates a document from version `1` to version `3` by applying two adjacent migrations.

## Use a custom version field

The default version field is `schemaVersion`. Pass a custom field name when your documents use a
different property.

```kotlin
schema(rootNode, versionField = "version") {
    migration(1, 2) {
        move("/name") to "/fullName"
    }
}
```

## Migrate documents without a version field

Missing version fields are rejected by default. Opt in only when you have a clear bootstrap case.

```kotlin
schema(rootNode, allowMissingVersionField = true) {
    migration(1, 2) {
        add("/enabled") with BooleanNode.TRUE
    }
}
```

After the migration completes, the configured version field is written with the target version.
