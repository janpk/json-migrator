package com.mosedotten.json.migrator.engine.test.operation

import com.mosedotten.json.migrator.engine.operation.Set
import com.mosedotten.json.migrator.engine.test.util.TestFixtures
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.BooleanNode

@DisplayName("When setting a field value")
internal class SetTest : TestFixtures() {
    @Test
    fun `expect success if it doesn't exist`() {
        assertMigrates(
            """{"name": "John Doe"}""",
            """{"name" : "John Doe", "newWithDefault" : true}""",
        ) {
            Set("/newWithDefault", BooleanNode.TRUE).apply(this)
        }
    }

    @Test
    fun `expect success if it exist`() {
        assertMigrates(
            """{"name" : "John Doe", "newWithDefault" : true}""",
            """{"name" : "John Doe", "newWithDefault" : false} """,
        ) {
            Set("/newWithDefault", BooleanNode.FALSE).apply(this)
        }
    }

    @Test
    fun `to missing parent creates nested path`() {
        assertMigrates(
            """{"name":"John Doe","age":30}""",
            """{"name":"John Doe","age":30,"contact":{"verified":true}}""",
        ) {
            Set("/contact/verified", BooleanNode.TRUE).apply(this)
        }
    }
}
