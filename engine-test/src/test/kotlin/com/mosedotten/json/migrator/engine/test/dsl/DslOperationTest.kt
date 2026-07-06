package com.mosedotten.json.migrator.engine.test.dsl

import com.mosedotten.json.migrator.engine.dsl.clause.add
import com.mosedotten.json.migrator.engine.dsl.clause.copy
import com.mosedotten.json.migrator.engine.dsl.clause.set
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
}
