package com.mosedotten.json.migrator.engine.test.dsl

import com.mosedotten.json.migrator.engine.dsl.clause.add
import com.mosedotten.json.migrator.engine.dsl.clause.copy
import com.mosedotten.json.migrator.engine.dsl.clause.createObject
import com.mosedotten.json.migrator.engine.dsl.clause.forEach
import com.mosedotten.json.migrator.engine.dsl.clause.merge
import com.mosedotten.json.migrator.engine.dsl.clause.move
import com.mosedotten.json.migrator.engine.dsl.clause.remove
import com.mosedotten.json.migrator.engine.dsl.clause.set
import com.mosedotten.json.migrator.engine.dsl.clause.split
import com.mosedotten.json.migrator.engine.test.util.JsonFixtures
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.BooleanNode

@DisplayName("When applying a DSL operation")
@Suppress("LargeClass") // One happy-path per DSL operation naturally accumulates; acceptable for test classes
internal class DslOperationTest : JsonFixtures() {

    @Test
    fun `add adds a field and bumps the version`() {
        assertSchemaMigrates(
            """{"schemaVersion":1,"name":"John Doe"}""",
            """{"schemaVersion":2,"name":"John Doe","enabled":true}""",
        ) {
            migration(1, 2) {
                add("/enabled") with BooleanNode.TRUE
            }
        }
    }

    @Test
    fun `set overwrites an existing field and bumps the version`() {
        assertSchemaMigrates(
            """{"schemaVersion":1,"name":"John Doe","enabled":false}""",
            """{"schemaVersion":2,"name":"John Doe","enabled":true}""",
        ) {
            migration(1, 2) {
                set("/enabled") with BooleanNode.TRUE
            }
        }
    }

    @Test
    fun `copy duplicates a field and bumps the version`() {
        assertSchemaMigrates(
            """{"schemaVersion":1,"id":"123","name":"John Doe"}""",
            """{"schemaVersion":2,"id":"123","name":"John Doe","legacyId":"123"}""",
        ) {
            migration(1, 2) {
                copy("/id") to "/legacyId"
            }
        }
    }

    @Test
    fun `remove deletes a field and bumps the version`() {
        assertSchemaMigrates(
            """{"schemaVersion":1,"name":"John Doe","deprecated":true}""",
            """{"schemaVersion":2,"name":"John Doe"}""",
        ) {
            migration(1, 2) {
                remove("/deprecated")
            }
        }
    }

    @Test
    fun `move relocates a field and bumps the version`() {
        assertSchemaMigrates(
            """{"schemaVersion":1,"name":"John Doe","age":30}""",
            """{"schemaVersion":2,"fullName":"John Doe","age":30}""",
        ) {
            migration(1, 2) {
                move("/name") to "/fullName"
            }
        }
    }

    @Test
    fun `merge combines fields and bumps the version`() {
        assertSchemaMigrates(
            """{"schemaVersion":1,"firstName":"John","lastName":"Doe"}""",
            """{"schemaVersion":2,"fullName":"John Doe"}""",
        ) {
            migration(1, 2) {
                merge("/firstName", "/lastName") into "/fullName"
            }
        }
    }

    @Test
    fun `split divides a field and bumps the version`() {
        assertSchemaMigrates(
            """{"schemaVersion":1,"fullName":"John Doe"}""",
            """{"schemaVersion":2,"firstName":"John","lastName":"Doe"}""",
        ) {
            migration(1, 2) {
                split("/fullName").into("/firstName", "/lastName")
            }
        }
    }

    @Test
    @Suppress("LongMethod") // Test methods are naturally longer
    fun `forEach applies operations to each array element and bumps the version`() {
        assertSchemaMigrates(
            """{"schemaVersion":1,"users":[{"name":"John"},{"name":"Jane"}]}""",
            """{"schemaVersion":2,"users":[{"fullName":"John"},{"fullName":"Jane"}]}""",
        ) {
            migration(1, 2) {
                forEach("/users") {
                    move("/name") to "/fullName"
                }
            }
        }
    }

    @Test
    fun `createObject ensures an object exists and bumps the version`() {
        assertSchemaMigrates(
            """{"schemaVersion":1,"name":"John"}""",
            """{"schemaVersion":2,"name":"John","address":{}}""",
        ) {
            migration(1, 2) {
                createObject("/address")
            }
        }
    }
}
