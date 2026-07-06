package com.mosedotten.json.migrator.engine.test.dsl

import com.mosedotten.json.migrator.engine.dsl.ExecutionStrategy
import com.mosedotten.json.migrator.engine.dsl.clause.add
import com.mosedotten.json.migrator.engine.dsl.clause.custom
import com.mosedotten.json.migrator.engine.dsl.clause.remove
import com.mosedotten.json.migrator.engine.dsl.clause.split
import com.mosedotten.json.migrator.engine.dsl.clause.transform
import com.mosedotten.json.migrator.engine.dsl.schema
import com.mosedotten.json.migrator.engine.exception.MigrationExecutionException
import com.mosedotten.json.migrator.engine.test.util.TestFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.node.BooleanNode
import tools.jackson.databind.node.IntNode

@DisplayName("When executing migrations atomically")
@Suppress("LongMethod") // Nested schema/migration DSL plus throw-and-assert makes these tests naturally long
internal class AtomicExecutionTest : TestFixtures() {

    @Test
    fun `a failed composite operation leaves the document unchanged`() {
        val root = obj("""{"schemaVersion":1,"fullName":"John Doe"}""")
        assertThrows<MigrationExecutionException> {
            schema(root) {
                migration(1, 2) {
                    split("/fullName").into("/name", "/name")
                }
            }
        }
        assertEquals(obj("""{"schemaVersion":1,"fullName":"John Doe"}"""), root)
    }

    @Test
    fun `disabling atomic execution leaves partial composite mutations`() {
        val root = obj("""{"schemaVersion":1,"fullName":"John Doe"}""")
        assertThrows<MigrationExecutionException> {
            schema(root, execution = ExecutionStrategy.NonAtomic) {
                migration(1, 2) {
                    split("/fullName").into("/name", "/name")
                }
            }
        }
        assertEquals(obj("""{"schemaVersion":1,"fullName":"John Doe","name":"John"}"""), root)
    }

    @Test
    fun `a throwing transform leaves the document unchanged`() {
        val root = obj("""{"schemaVersion":1,"age":30}""")
        assertThrows<IllegalStateException> {
            schema(root) {
                migration(1, 2) {
                    add("/added") with BooleanNode.TRUE
                    transform("/age") { error("boom") }
                }
            }
        }
        assertEquals(obj("""{"schemaVersion":1,"age":30}"""), root)
    }

    @Test
    fun `a throwing custom operation leaves the document unchanged`() {
        val root = obj("""{"schemaVersion":1,"name":"John"}""")
        assertThrows<IllegalStateException> {
            schema(root) {
                migration(1, 2) {
                    add("/added") with BooleanNode.TRUE
                    custom { error("boom") }
                }
            }
        }
        assertEquals(obj("""{"schemaVersion":1,"name":"John"}"""), root)
    }

    @Test
    fun `a later migration failure rolls back earlier migrations`() {
        val root = obj("""{"schemaVersion":1,"name":"John"}""")
        assertThrows<MigrationExecutionException> {
            schema(root) {
                migration(1, 2) {
                    add("/a") with BooleanNode.TRUE
                }
                migration(2, 3) {
                    remove("/missing")
                }
            }
        }
        assertEquals(obj("""{"schemaVersion":1,"name":"John"}"""), root)
    }

    @Test
    fun `disabling atomic execution keeps earlier successful migrations`() {
        val root = obj("""{"schemaVersion":1,"name":"John"}""")
        assertThrows<MigrationExecutionException> {
            schema(root, execution = ExecutionStrategy.NonAtomic) {
                migration(1, 2) {
                    add("/a") with BooleanNode.TRUE
                }
                migration(2, 3) {
                    remove("/missing")
                }
            }
        }
        assertEquals(obj("""{"schemaVersion":2,"name":"John","a":true}"""), root)
    }

    @Test
    fun `a successful atomic migration applies normally`() {
        val root = obj("""{"schemaVersion":1,"age":30}""")
        schema(root) {
            migration(1, 2) {
                transform("/age") { IntNode.valueOf(asInt() + 1) }
            }
        }
        assertEquals(obj("""{"schemaVersion":2,"age":31}"""), root)
    }
}
