package com.mosedotten.json.migrator.engine.test.dsl

import com.mosedotten.json.migrator.engine.dsl.clause.add
import com.mosedotten.json.migrator.engine.dsl.schema
import com.mosedotten.json.migrator.engine.exception.ExistingFieldException
import com.mosedotten.json.migrator.engine.exception.MigrationExecutionException
import com.mosedotten.json.migrator.engine.test.util.JsonFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.node.BooleanNode
import tools.jackson.databind.node.IntNode

@DisplayName("When running the schema DSL")
internal class DslSchemaTest : JsonFixtures() {

    @Test
    @Suppress("LongMethod") // Test methods are naturally longer
    fun `multiple migrations chain in order over the same node`() {
        assertSchemaMigrates(
            """{"schemaVersion":1,"name":"John Doe"}""",
            """{"schemaVersion":3,"name":"John Doe","enabled":true,"contact":{"verified":true}}""",
        ) {
            migration(1, 2) {
                add("/enabled") with BooleanNode.TRUE
            }
            migration(2, 3) {
                add("/contact/verified") with BooleanNode.TRUE
            }
        }
    }

    @Test
    fun `an empty schema returns the node untouched`() {
        assertSchemaMigrates(
            """{"schemaVersion":1,"name":"John Doe"}""",
            """{"schemaVersion":1,"name":"John Doe"}""",
        ) {}
    }

    @Test
    fun `a missing version field is allowed when opted in`() {
        assertSchemaMigrates(
            """{"name":"John Doe"}""",
            """{"name":"John Doe","enabled":true,"schemaVersion":2}""",
            allowNoVersionField = true,
        ) {
            migration(1, 2) {
                add("/enabled") with BooleanNode.TRUE
            }
        }
    }

    @Test
    @Suppress("LongMethod")
    fun `an operation error aborts the migration and includes migration context`() {
        assertThrows<MigrationExecutionException> {
            schema(obj("""{"schemaVersion":1,"name":"John Doe"}""")) {
                migration(1, 2) {
                    add("/name") with IntNode.valueOf(30)
                }
            }
        }.also {
            assertEquals(1, it.fromVersion)
            assertEquals(2, it.toVersion)
            assertEquals(1, it.operationIndex)
            assertEquals("add(\"/name\")", it.operationDescription)
            assertEquals(ExistingFieldException::class, it.failure::class)
        }
    }
}
