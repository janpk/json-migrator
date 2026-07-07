package com.mosedotten.json.migrator.engine.test.dsl

import com.mosedotten.json.migrator.engine.dsl.clause.add
import com.mosedotten.json.migrator.engine.dsl.schema
import com.mosedotten.json.migrator.engine.exception.MigrationVersionException
import com.mosedotten.json.migrator.engine.test.util.TestFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.node.BooleanNode

@DisplayName("When the schema DSL validates versions, then")
internal class DslVersionValidationTest : TestFixtures() {

    @Test
    fun `a migration can not migrate from version 0`() {
        assertThrows<MigrationVersionException> {
            schema(obj("""{"schemaVersion":0,"name":"John Doe"}""")) {
                migration(0, 1) {
                    add("/enabled") with BooleanNode.TRUE
                }
            }
        }
    }

    @Test
    fun `a migration can not migrate to version 0`() {
        assertThrows<MigrationVersionException> {
            schema(obj("""{"schemaVersion":0,"name":"John Doe"}""")) {
                migration(1, 0) {
                    add("/enabled") with BooleanNode.TRUE
                }
            }
        }
    }

    @Test
    fun `a missing version field is rejected by default`() {
        assertThrows<MigrationVersionException> {
            schema(obj("""{"name":"John Doe"}""")) {
                migration(1, 2) {
                    add("/enabled") with BooleanNode.TRUE
                }
            }
        }
    }

    @Test
    fun `a migration whose from is behind the document version is skipped`() {
        val root = obj("""{"schemaVersion":5,"name":"John Doe"}""")
        schema(root) {
            migration(1, 2) {
                add("/enabled") with BooleanNode.TRUE
            }
        }
        assertEquals(obj("""{"schemaVersion":5,"name":"John Doe"}"""), root)
    }

    @Test
    fun `a forward gap in versions is rejected`() {
        assertThrows<MigrationVersionException> {
            schema(obj("""{"schemaVersion":1,"name":"John Doe"}""")) {
                migration(3, 4) {
                    add("/enabled") with BooleanNode.TRUE
                }
            }
        }
    }

    @Test
    fun `a downgrade whose from is ahead of the document version is skipped`() {
        val root = obj("""{"schemaVersion":1,"name":"John Doe"}""")
        schema(root) {
            migration(2, 1) {
                add("/enabled") with BooleanNode.TRUE
            }
        }
        assertEquals(obj("""{"schemaVersion":1,"name":"John Doe"}"""), root)
    }

    @Test
    fun `a backward gap in versions is rejected`() {
        assertThrows<MigrationVersionException> {
            schema(obj("""{"schemaVersion":3,"name":"John Doe"}""")) {
                migration(2, 1) {
                    add("/enabled") with BooleanNode.TRUE
                }
            }
        }
    }

    @Test
    fun `a non-integer version field is rejected`() {
        assertThrows<MigrationVersionException> {
            schema(obj("""{"schemaVersion":"1","name":"John Doe"}""")) {
                migration(1, 2) {
                    add("/enabled") with BooleanNode.TRUE
                }
            }
        }
    }

    @Test
    fun `non-adjacent from and to versions are rejected`() {
        assertThrows<MigrationVersionException> {
            schema(obj("""{"schemaVersion":1}""")) {
                migration(1, 3) {
                    add("/enabled") with BooleanNode.TRUE
                }
            }
        }
    }
}
